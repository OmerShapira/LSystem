package lSystem;

/* Processing - Internal */
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

/* Processing - OpenGL */

import processing.opengl.*; 
import javax.media.opengl.GL;
import java.nio.*;

/* Processing Packages */

import oscP5.*;
import netP5.*;

/* Java */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class LSystemGL extends PApplet{	

	/* OpenGL & Buffers */
	PGraphicsOpenGL pgl;
	GL gl;
	FloatBuffer fBuffer, gBuffer;

	/* Coordinates */
	float startX, startY;
	int clickX,clickY = 0;


	/* Constants */
	float 	//Basic vertex length

	//TODO find out
	weight = 0.7f,
	//TODO find out
	scale = 0.99f, 
	//TODO undeprecate
	zRot = 30f,
	//
	windSpeed = 1/3000f,
	//
	windDamper = 0.3f,
	//Current grass height
	//	grassHeight = 10f,
	// max (pixels) grass height
	MAX_GRASS_HEIGHT = 150f,
	spreadFactor = 1.2f,
	growthStatus = 0.1f,
	MIN_LENGTH = 1f,
	MAX_LENGTH = 20f,
	MIN_GROWTH = -0.06f,
	MAX_GROWTH = 0.09f;



	int 	//current detph
	depth =	4,
	maxDepth = 5,
	NUM_TREES = 6,
	GRASS_INDEX = NUM_TREES; 


	/* Physics */ 
	boolean treeWindOn = true;
	PVector grassWind = new PVector(0,0);
	float[] length;
	float[] growth;
	float shrinkFactor = -.003f;
	float growthFactor = .005f;


	/* GUI */
	boolean help = false;
	boolean hud = false;

	/* Random */
	Random rand = new Random();
	//TODO replace with perlin noise
	float jitter = 0;


	/* Sets */
	//	Queue<Queue<Float> > vertexQueues = new LinkedList<Queue<Float> >();
	Queue<Float> vertexQueue = new LinkedList<Float>();
	HashSet<Character> constants= new HashSet<Character>(Arrays.asList('[',']'));
	char[] vocab = {'F','F','F','-','+','#','#','*','*','^','_'}; //Duplicates added to increase probabilities
	HashMap<Character,String> ruleset = new HashMap<Character, String>();
	ArrayList<String> evaluatedStrings = new ArrayList<String>();
	//TODO better tree rules
	String[] fRules = {	
			"F[F][+F]F[-F]F", 
			//			"F[+F+F]#[-F]-F+",
			//			"F[-FF+F][+FF-F]FF",
			//			"[-F][+F]F+F-F+F-",
			//			"F[+F]*[-F]",
			"F[F][+F]F[-F]",
			"F[F][+F]*F[-F]" //The random rule
	};
	String[] prefetchedRules = new String[NUM_TREES*2];
	ArrayList<Grass> grass = new ArrayList<Grass>();



	//Interaction
	OscP5 oscP5;

	//	ArrayList<Gesture>gestures = new ArrayList<Gesture>();
	/* Processing Loop */

	public void setup(){
		//Sketch setup
		size (screen.width,screen.height , OPENGL);

		//OpenGL setup
		hint(DISABLE_DEPTH_TEST);
		hint(DISABLE_OPENGL_ERROR_REPORT);
		//		hint(ENABLE_OPENGL_2X_SMOOTH);
		pgl = (PGraphicsOpenGL) g; //g is PApplet's native PGraphics child
		gl = pgl.gl;
		gl.glEnable(gl.GL_LINE_SMOOTH);
		gl.glEnable(gl.GL_BLEND);
		gl.setSwapInterval(0); //tells it not to sync with the screen refresh rate

		//Sketch specs
		frameRate(30);
		rectMode(CORNER);
		//      textMode(SCREEN);
		noFill();

		/* Init */
		//Feed to tree starting point
		startX = 2*width/(2f*(NUM_TREES+2));
		startY = height; //height*7/8f;
		translate(0,100,0);

		/* Init Physics */
		growth = new float[NUM_TREES+1];
		length = new float[NUM_TREES+1];
		for (int i = 0 ; i < NUM_TREES+1 ; i++){ 
			growth[i] = 0f;
			length[i] = 1f;	
		}

		/* Inputs */
		oscP5 = new OscP5(this,5001);

		/* Populate Grass */
		for (int i = 0 ; i < 90 ; i++){
			grass.add(new Grass(50, width/2, 0.5f, this));
		}

		/* Populate Trees */
		for (int i = 0 ; i< NUM_TREES*2 ;  i++){
			loadRule(i);
			prefetchedRules[i] = getNthIteration(depth);
		}

	}

	public void draw(){
		
		background (100);

		pushMatrix();
		translate(startX ,startY);
		//		translate(0,100);

		updateGrowth();

		for (Grass g: grass) {
			g.feedToQueue(vertexQueue, 50*length[GRASS_INDEX], 10, width, height);
			g.feedToQueue(vertexQueue, 50*length[GRASS_INDEX], 10, width/2, height);
			g.feedToQueue(vertexQueue, 50*length[GRASS_INDEX], 10, width/3, height);
			g.feedToQueue(vertexQueue, 50*length[GRASS_INDEX], 10, 2*width/3, height);
		}

		//Update global jitter value
		jitter = !treeWindOn ? 0 : (-.5f+noise(millis()*windSpeed ))*windDamper; 

		//Fill The Vertex List with several trees
		for (int i=0; i<NUM_TREES; i++){
			pushMatrix();
			drawString(prefetchedRules[i],length[i], weight, false);
			popMatrix();
			translate(width*spreadFactor/(NUM_TREES+2f),0);
			
/*			pushMatrix();
			pushStyle();
			translate(0,-50,400);
			stroke(20,20,20,100);
			drawString(prefetchedRules[2*i],length[i], weight, false);
			popMatrix();
			popStyle(); */
		}

		//return for next iteration
		popMatrix();

		//Allocate buffer and fill with vertices 
		fBuffer = ByteBuffer.allocateDirect(4*vertexQueue.size()).order(ByteOrder.nativeOrder()).asFloatBuffer();
		for (Float fl : vertexQueue){
			fBuffer.put(fl.floatValue());
		}

		//Draw to GL		
		fBuffer.rewind();
		drawFromBuffer(pgl,gl,fBuffer, 3, true);


		//Clear the list
		vertexQueue.clear();

		//text overlay
		if (hud) 	drawHUD();
		if (help) 	drawHelp();
	}


	/* Graphics  */

	/**
	 * Draws the HUD overlay
	 */
	private void drawHUD() {
		textSize(12);
		pushMatrix();
		translate(30,30);
		text(String.format("Length: %1$.1f\nDecay: %2$.1f\nAngle: %3$.0f", length, scale, zRot), 0,0);

		//rules
		StringBuffer ruleString = new StringBuffer();
		for (int i = 0 ; i < 3 ; i++ ){
			String e = evaluatedStrings.get(i);
			ruleString.append(wrap(e,80));
		}
		text(ruleString.toString(), 120, 0);
		popMatrix();

		//rule overlay
		textSize(12);
		pushMatrix(); 
		translate(width-180, 20);
		text("Rule",0,0);
		text("Depth:",80,0);
		textSize(36);
		text(depth,85,50);
		translate(15,50);

		popMatrix();


	}


	/**
	 * Draws the help bar overlay
	 */
	private void drawHelp(){
		textSize(10);
		String instructions = "<0-9> : Rules | <a,s> : Steps | <q,w>,<Q,W> : Angles" +
				" | <d,f> : Size | <D,F> : Decay | <h,j> : Help / HUD | Mouse drag: Move | Shift+drag: Scale";
		text(instructions, 20, height-15);
	}

	/**
	 * Draws the vertex buffer with GL_LINES.
	 * Automatically begins and ends GL 
	 * @param pgl2 the PGraphics GL element
	 * @param gl2 the GL element
	 * @param f2 The floatBuffer object containing vertices.
	 * @param numShadows The number of shadows to draw on screen
	 * @param b
	 */
	private void drawFromBuffer(PGraphicsOpenGL pgl2, GL gl2, FloatBuffer f2,
			int numShadows, boolean b) {
		//Open stream
		pgl2.beginGL();
		gl2.glEnableClientState(GL.GL_VERTEX_ARRAY);
		gl2.glVertexPointer(2,GL.GL_FLOAT,0,fBuffer);

		gl2.glPushMatrix();

		//Define Main Stroke
		gl2.glColor4f(0.1f,0.1f,0.1f,.6f);
		gl2.glLineWidth(1.0f);
		//Draw
		gl2.glDrawArrays(GL.GL_LINES,0,fBuffer.capacity()/2);

		//Draw Shadows
		for(int i=0; i<numShadows ; i++){
			gl2.glLineWidth(1.0f+1.8f*i);
			gl2.glTranslatef(
					(-.5f+sin(noise(millis()*windSpeed*3 - i*40)))*12,
					(-3.0f+noise(millis()*windSpeed*3 +i*40))*12
					,50f);
			//			gl2.glRotatef(0f,10f,10f,40f);
			gl2.glColor4f(0.1f,0.1f,0.1f,(.1f*scale-0.02f*i));
			gl2.glDrawArrays(GL.GL_LINES,0,fBuffer.capacity()/2);  
		}
		//Correct all the translations
		gl2.glPopMatrix();

		//Close stream
		gl2.glDisableClientState(GL.GL_VERTEX_ARRAY);
		pgl2.endGL();

		//Debug status note
		println(frameRate);
	}

	/**
	 * 
	 * @param pg 
	 * @param s A string with terminating values
	 */
	void drawString(String s, float l, float w, boolean icon){	
		//print("Evaluating: "+s);
		LinkedList<Integer> counters = new LinkedList<Integer>();
		int stackCounter = 0;

		char[] chars = s.toCharArray();		
		for (char c : chars){
			switch (c){
			case '[':{ 	
				pushMatrix();
				counters.push(stackCounter);
				stackCounter = 0;
				break;	
			}
			case ']':{	while (stackCounter>0) {
				popMatrix();
				stackCounter--;
			}
			stackCounter=counters.pop();
			popMatrix();
			break;	
			}
			case '+':{	
				rotate(radians(zRot)+jitter/counters.size() +noise(millis()*windSpeed + counters.size()*(stackCounter + 3)*depth)*windDamper );
				stackCounter++;
				pushMatrix();
				break;	
			}
			case '-':{	
				rotate(radians(-zRot)+jitter/counters.size() +noise(millis()*windSpeed + counters.size()*(stackCounter + 3)*depth)*windDamper );
				stackCounter++;
				pushMatrix();	
				break;	
			}
			case '#':{	  //Mirror
				scale(-1,1);
				stackCounter++;
				pushMatrix();
				break;
			}
			case '_':{	 //Scale
				scale(scale,scale);
				stackCounter++;
				pushMatrix();
				break;
			}
			case '^':{	 //reverse scale
				scale(1f/scale,1f/scale);
				stackCounter++;
				pushMatrix();
				break;
			}
			case 'F':{	
				float damper = icon? 1 : (float) (Math.pow(scale, depth)); //TODO figure out what to do with the damper
				//			strokeWeight(w*damper);
				//line(0,0,0,-l*damper); //This is used for shortening branches
				//			line(0,0,0,-l);
				vertexQueue.add(screenX(0,0,0));
				vertexQueue.add(screenY(0,0,0));
				vertexQueue.add(screenX(0,-l,0));
				vertexQueue.add(screenY(0,-l,0));

				//translate(0,-l*damper); //Shortening branches
				translate(0,-l);
				pushMatrix();
				stackCounter++;
				break;	
			}
			default:{
				break;	
			}
			}
			while (stackCounter>0){
				popMatrix();
				stackCounter--;
			}
		}
	}

	/* Physics */

	private void updateGrowth() {
		for (int i  = 0 ; i < growth.length ; i++){
			length[i] += growth[i];
			print(i+": "+growth[i]+", ");
			if ((length[i] > MIN_LENGTH) && (length[i] <MAX_LENGTH)){ 
//								length[i] += growth[i];
			} else {
//								growth[i] = 0f;
								length[i] = (length[i] <= MIN_LENGTH ? MIN_LENGTH : MAX_LENGTH) ;
			}
		}
		println();
	}


	/* Local Control  */


	public void keyPressed(){
		switch (key){
		case 's':{	depth = (depth<maxDepth)? depth+1 : depth;  
		break;	}
		case 'a':{	depth = (depth>1)? depth-1 : depth;
		break;	}
		case 'w':{	zRot += 1;
		break;	}
		case 'W':{	zRot += 10;
		break; }
		case 'q':{	zRot -= 1;
		break;	}
		case 'Q':{	zRot -= 10;
		break; }
		/*
		case 'f':{	length *= 1.1f;
		break;	}
		case 'd':{	length *= 0.9f;
		break;	}
		 */
		case 'F':{scale += .1f;
		break;
		}
		case 'D':{	scale -= .1f;
		break;
		}
		case 'h':{ 	help=!help;
		break;	}
		case 'j':{ hud=!hud;
		break;	}
		case 'v' :{
			length[GRASS_INDEX] *= 1.1f;
			break;
		}
		case 'c' :{
			length[GRASS_INDEX] *= 0.9f;
			break;
		}
		default:{	break; 	}
		}
		switch (keyCode){
		case UP:{	startY -= 10;
		break;	}
		case DOWN:{	startY += 10;
		break;	}
		case RIGHT:{startX += 10;
		break;	}
		case LEFT:{	startX -= 10;
		break;	}
		default: {	break;	}
		}

		if (key >= '0' && key <= '9'){
			grow(((int)key)%NUM_TREES, growthFactor);
//			println("Growing: "+(((int)key)%NUM_TREES));
		}

//		redraw();
	}
	public void mousePressed(){

		clickX = mouseX;
		clickY = mouseY;
	}
	/*
	public void mouseDragged(){
		if (keyPressed && keyCode==SHIFT){
			length+=(clickY - mouseY)/5f;
			length = Math.abs(length);
		} else {
			startX += (clickX - mouseX)/2;
			startY += (clickY - mouseY)/2; 
		}
		mousePressed();
		redraw();
	}

	 */

	/* OSC & Remote Control  */

	void oscEvent(OscMessage theOscMessage){
		String type = theOscMessage.addrPattern();
		int gesture = theOscMessage.get(0).intValue();
		float delta = theOscMessage.get(1).floatValue();
		char command = type.charAt(type.length()-1);
		println("Incoming message | Tag : "+type+", State : "+gesture+", delta:"+delta);
		//Manually Written instructions

		switch (command){
		case '1':{
			inner: switch (gesture) {
			case 0 : {
				for (int i = 0 ; i< 3 ; i++){
					grow(i, shrinkFactor);
				}
				break inner;
			} default : {
				grow(gesture-1, growthFactor); //TODO can add the strength
				for (int i = 0 ; i < 3; i++){
					if (i!=(gesture-1)) {grow(i, shrinkFactor);}
				}
				break inner;
			}

			}
		break;
		}
		case '2':{
			inner: switch (gesture) {
			case 0 : {
				for (int i = 3 ; i< 6 ; i++){
					grow(i, shrinkFactor);
				}
				break inner;
			} default : {
				grow(gesture+3-1, growthFactor); //TODO can add the strength
				for (int i = 3 ; i < 6; i++){
					if ((i-3)!=(gesture-1)) {grow(i, shrinkFactor);}
				}
				break inner;
			}

			}
		break;
		}	case '3':{ //Grass
			if (gesture == 1) {
				grow(GRASS_INDEX, delta*0.005f);
			} else {
				grow(GRASS_INDEX, shrinkFactor);
			}
		}
		default: break;

		}

	}

	private void grow(int index){
		grow (index, growthFactor);
	}	
	private void grow(int index, float value){
		if ((growth[index]+ value > MIN_GROWTH ) && (growth[index] + value < MAX_GROWTH ) ){ //TODO Check this
			if (value > 0) { //trying to raise value, at max when furthest away from max growth.
//				growth[index] += value*pow((abs(growth[index] - MAX_GROWTH)),1.5f);
				growth[index] += value;
			} else {
//				growth[index] += value*pow((abs(growth[index] - MIN_GROWTH)),1.5f);
				growth[index] += value;
			}	
		} else {
			growth[index] *= 0.5;
		}
	}

	/* String Manipulators */


	/**
	 * String expansion, single
	 */
	String expandRule(String input, HashMap<Character,String> rules, boolean evolve){
		StringBuffer output = new StringBuffer();
		char[] chars = input.toCharArray();
		for (char c : chars){
			String d;
			if (rules.containsKey(c)){
				d=rules.get(c);
			} else if (evolve){
				d=str(mutateChar(c));
			} else {
				d=str(c);
			}
			output.append(d);
		}
		return output.toString(); 
	}

	/**
	 * 	Memoization of the String Expansion
	 */
	String getNthIteration(int n){
		//Range check
		if (n < 1) n = 1;
		if (n > maxDepth) n = maxDepth;
		//Memoized?
		for (int i=(evaluatedStrings.size());i <= n; i++){
			evaluatedStrings.add(expandRule(evaluatedStrings.get(i-1), ruleset,true));
		}
		return evaluatedStrings.get(n);
	}

	/**
	 * Load rule string and evaluate the entire string expansion (up to maxDepth)
	 * @param i the rule number
	 */
	private void loadRule(int i) {
		ruleset.put('F', fRules[i%(fRules.length)]); //modulo in order to wrap the rule order.
		evaluatedStrings.clear(); //re-evaluate every time a string loads; clear the current cache 
		evaluatedStrings.add("F");
		getNthIteration(maxDepth);		
	}

	/**
	 * Line-wrapping function
	 * @param s the input string
	 * @param maxLength the line wrapper
	 * @return
	 */
	public String wrap(String s, int maxLength){
		StringBuffer wrapped = new StringBuffer();
		for (int i = 0; i*maxLength<s.length(); i++){
			wrapped.append(s.substring(i*maxLength, min((s.length()),(i+1)*maxLength))+"\n");
		}
		return wrapped.substring(0,wrapped.length());
	}

	/**
	 * Randomizes wildcard characters
	 * @param c the input char.
	 * @return
	 */
	private char mutateChar(char c) {
		return (c == '*')? 
				vocab[abs(rand.nextInt())%vocab.length] 
						: c;
	}




	public static void main(String[] args){
		PApplet.main(new String[] { "--present", "lSystem.LSystemGL" });
	}
}
