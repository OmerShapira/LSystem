package lSystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

public class LSystem2DRunner extends PApplet{

	/* Cards */
	PGraphics card, thumbnail;

	/* Coordinates */
	float startX, startY;
	int clickX,clickY = 0;

	/* Constants */
	float 	length = 7f,
			weight = 0.7f,
			scale = 0.9f, 
			zRot = 30f,
			windSpeed = 1/2000f,
			windDamper = 0.3f;
	int 	depth =	3,
			maxDepth = 7;
	boolean wind = true;

	/* GUI */
	boolean help = true;
	boolean hud = true;

	/* Sets */
	HashSet<Character> constants= new HashSet<Character>(Arrays.asList('[',']'));
	char[] vocab = {'F','F','F','-','+','#','#','*','*','^','_'};
	HashMap<Character,String> ruleset = new HashMap<Character, String>();
	ArrayList<String> evaluatedStrings = new ArrayList<String>();
	String[] fRules = {	
			"F[F][+F]F[-F]F", 
			"F[+F+F][-F]-F+",
			"F[-FF+F][+FF-F]FF",
			"[-F][+F]F+F-F+F-",
			"F[+F]*[-F]",
			"F[F][+F]F[-F]",
			"F[F][+F]*F[-F]",
			"****[**]**[**[**]**]**",
			"F#[+F]"
	};

	/* Random */
	Random rand = new Random();
	//TODO replace with perlin noise
	float jitter = 0;


	public void setup(){


		/* Processing */
		size(1000,800, P3D);
		thumbnail = createGraphics(80,80, P3D);
		card = createGraphics(width, height, P3D);
		frameRate(25);
		//		noLoop();
		rectMode(CORNER);
		textMode(SCREEN);


		//TODO Apply for both pgraphics


		/* Init */
		loadRule(0);

		startX = width/2f;
		startY = height*2/3f;

	}


	public void draw(){
		background(70);
		//move turtle to starting point		
		card.beginDraw();
		card.pushMatrix();

		card.background(0,0);

		card.translate(startX ,startY);
		initPG(card);
		jitter = -.5f+noise(millis()*windSpeed )*windDamper;  //TODO Make jitter parametric.
		drawString(card, getNthIteration(depth),length, weight, false);

		//return for next iteration
		card.popMatrix();
		card.endDraw();
		imageMode(CORNER);
		PImage img = card.get();

		card.pushMatrix();
		card.rotateX(radians(60));
		//		image(card.get(),0,0);
		//		blend(card.get(), 0,0,width,height,0,0,width,height,SCREEN);
		card.popMatrix();
		blend(img, 0,0,width,height,0,0,width,height,SCREEN);
		//		image(img, 0,0);


		//text overlay left
		if (hud){
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
			thumbnail.beginDraw();
			drawString(thumbnail, ruleset.get('F'), 15, 1.5f, true);
			thumbnail.endDraw();
			popMatrix();
		}
		if (help) {
			textSize(10);
			String instructions = "<0-9> : Rules | <a,s> : Steps | <q,w>,<Q,W> : Angles" +
					" | <d,f> : Size | <D,F> : Decay | <h,j> : Help / HUD | Mouse drag: Move | Shift+drag: Scale";
			text(instructions, 20, height-15);
		}
	}

	private void initPG(PGraphics pg) {
		pg.stroke(200);
		pg.smooth();
		pg.noFill();
		//		pg.rotate(radians(180)); //TODO move away from here
	}


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
	 * 
	 * @param pg 
	 * @param s A string with terminating values
	 */
	void drawString(PGraphics pg, String s, float l, float w, boolean icon){	
		//print("Evaluating: "+s);
		LinkedList<Integer> counters = new LinkedList<Integer>();
		int stackCounter = 0;

		char[] chars = s.toCharArray();		
		for (char c : chars){
			switch (c){
			case '[':{ 	
				pg.pushMatrix();
				counters.push(stackCounter);
				stackCounter = 0;
				break;	
			}
			case ']':{	while (stackCounter>0) {
				pg.popMatrix();
				stackCounter--;
			}
			stackCounter=counters.pop();
			pg.popMatrix();
			break;	
			}
			case '+':{	
				pg.rotate(radians(zRot)+  
						(!wind? 0 : jitter/counters.size() 
								+noise(millis()*windSpeed + counters.size()*(stackCounter + 3)*depth)*windDamper)
						);
				stackCounter++;
				pg.pushMatrix();
				break;	
			}
			case '-':{	
				pg.rotate(radians(-zRot)+
						(!wind? 0 : jitter/counters.size() +noise(millis()*windSpeed + counters.size()*(stackCounter + 3)*depth)*windDamper )
						);
				stackCounter++;
				pg.pushMatrix();	
				break;	
			}
			case '#':{	
				pg.scale(-1,1);
				stackCounter++;
				pg.pushMatrix();
				break;
			}
			case '_':{	
				pg.scale(scale,scale);
				stackCounter++;
				pg.pushMatrix();
				break;
			}
			case '^':{	
				pg.scale(1f/scale,1f/scale);
				stackCounter++;
				pg.pushMatrix();
				break;
			}
			case 'F':{	
				float damper = icon? 1 : (float) (Math.pow(scale, depth));
				pg.strokeWeight(w*damper);
				//line(0,0,0,-l*damper);
				pg.line(0,0,0,-l);
				//translate(0,-l*damper);
				pg.translate(0,-l);
				pg.pushMatrix();
				stackCounter++;
				break;	
			}
			default:{
				break;	
			}
			}
			while (stackCounter>0){
				pg.popMatrix();
				stackCounter--;
			}
		}
	}

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
		case 'f':{	length += 1;
		break;	}
		case 'd':{	length -= 1;
		break;	}
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
		case 'p':{ wind = !wind;
					break;}
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
			loadRule((int)(key));
		}

		redraw();
	}
	public void mousePressed(){
		clickX = mouseX;
		clickY = mouseY;
	}
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


	private void loadRule(int i) {
		ruleset.put('F', fRules[i%(fRules.length)]);
		evaluatedStrings.clear();
		evaluatedStrings.add("F");
		getNthIteration(maxDepth);		
	}

	public String wrap(String s, int maxLength){
		StringBuffer wrapped = new StringBuffer();
		for (int i = 0; i*maxLength<s.length(); i++){
			wrapped.append(s.substring(i*maxLength, min((s.length()),(i+1)*maxLength))+"\n");
		}
		return wrapped.substring(0,wrapped.length());
	}

	//	public static void main(String[] args){
	//		PApplet.main(new String[] { "--present", "lSystem.LSystem2DRunner" });
	//	}

	private char mutateChar(char c) {
		return (c == '*')? 
				vocab[abs(rand.nextInt())%vocab.length] 
						: c;
	}

}
