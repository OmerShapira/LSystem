

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;



/* Coordinates */
float startX, startY;
int clickX, clickY = 0;

/* Constants */
float 	length = 7f, 
weight = 0.7f, 
scale = 0.6f, 
zRot = 30f;
int depth =	3, 
maxDepth = 7;

/* GUI */
boolean help = true;
boolean hud = true;

/* Sets */
HashSet<Character> constants= new HashSet<Character>(Arrays.asList('[', ']', '+', '-'));
HashSet<Character> vocab = new HashSet<Character>('F');
HashMap<Character, String> ruleset = new HashMap<Character, String>();
ArrayList<String> evaluatedStrings = new ArrayList<String>();
String[] fRules = {	
  "F[F][+F]F[-F]F", 
  "F[+F+F][-F]-F+", 
  "F[-FF]+F", 
  "F[-FF+F][+FF-F]FF", 
  "[-F][+F]F+F-F+F-", 
  "F[+F]F[-F][-F][F]F", 
  "F[+FF][-FF]"
};


public void setup() {


  /* Processing */
  size(1000, 800);
  frameRate(10);
  stroke(200);
  noLoop();
  smooth();
  rectMode(CORNER);
  noFill();
  //TODO Make icon from rules
  rotate(radians(180));



  /* Init */
  loadRule(0);

  startX = width/2f;
  startY = height*2/3f;
}


public void draw() {
  //move turtle to starting point
  background(70);
  pushMatrix();
  translate(startX, startY);

  drawString(getNthIteration(depth), length, weight, false);

  //return for next iteration
  popMatrix();

  //text overlay left
  if (hud) {
    textSize(12);
    pushMatrix();
    translate(30, 30);
    text(String.format("Length: %1$.1f\nDecay: %2$.1f\nAngle: %3$.0f", length, scale, zRot), 0, 0);

    //rules
    StringBuffer ruleString = new StringBuffer();
    for (int i = 0 ; i < 3 ; i++ ) {
      String e = evaluatedStrings.get(i);
      ruleString.append(wrap(e, 80));
    }
    text(ruleString.toString(), 120, 0);
    popMatrix();

    //rule overlay
    textSize(12);
    pushMatrix();
    translate(width-180, 20);
    text("Rule", 0, 0);
    text("Depth:", 80, 0);
    textSize(36);
    text(depth, 85, 50);
    translate(15, 50);
    drawString(ruleset.get('F'), 15, 1.5f, true);
    popMatrix();
  }
  if (help) {
    textSize(10);
    String instructions = "<0-9> : Rules | <a,s> : Steps | <q,w>,<Q,W> : Angles" +
      " | <d,f> : Size | <D,F> : Decay | <h,j> : Help / HUD | Mouse drag: Move | Shift+drag: Scale";
    text(instructions, 20, height-15);
  }
}

/**
 	 * String expansion, single
 	 */
String expandRule(String input, HashMap<Character, String> rules) {
  StringBuffer output = new StringBuffer();
  char[] chars = input.toCharArray();
  for (char c : chars) {
    if (rules.containsKey(c)) {
      output.append(rules.get(c));
    } 
    else {
      output.append(c);
    }
  }
  return output.toString();
}
/**
 	 * 	Memoization of the String Expansion
 	 */
String getNthIteration(int n) {
  //Range check
  if (n < 1) n = 1;
  if (n > maxDepth) n = maxDepth;
  //Memoized?
  for (int i=(evaluatedStrings.size());i <= n; i++) {
    evaluatedStrings.add(expandRule(evaluatedStrings.get(i-1), ruleset));
  }
  return evaluatedStrings.get(n);
}

/**
 	 * 
 	 * @param s A string with terminating values
 	 */
void drawString(String s, float l, float w, boolean icon) {	
  //print("Evaluating: "+s);
  LinkedList<Integer> counters = new LinkedList<Integer>();
  int stackCounter = 0;
  char[] chars = s.toCharArray();
  for (char c : chars) {
    switch (c) {
    case '[':
      { 	
        pushMatrix();
        counters.push(stackCounter);
        stackCounter = 0;
        break;
      }
    case ']':
      {	
        while (stackCounter>0) {
          popMatrix();
          stackCounter--;
        }
        stackCounter=counters.pop();
        popMatrix();
        break;
      }
    case '+':
      {	
        rotate(radians(zRot));
        stackCounter++;
        pushMatrix();
        break;
      }
    case '-':
      {	
        rotate(-radians(zRot));
        stackCounter++;
        pushMatrix();	
        break;
      }
    case 'F':
      {	
        float damper = icon? 1 : (float) (Math.pow(scale, depth));
        strokeWeight(w*damper);
        //line(0,0,0,-l*damper);
        line(0, 0, 0, -l);
        //translate(0,-l*damper);
        translate(0, -l);
        pushMatrix();
        stackCounter++;
        break;
      }
    default:
      {	
        break;
      }
    }
    while (stackCounter>0) {
      popMatrix();
      stackCounter--;
    }
  }
}

public void keyPressed() {
  switch (key) {
  case 's':
    {	
      depth = (depth<maxDepth)? depth+1 : depth;  
      break;
    }
  case 'a':
    {	
      depth = (depth>1)? depth-1 : depth;
      break;
    }
  case 'w':
    {	
      zRot += 1;
      break;
    }
  case 'W':
    {	
      zRot += 10;
      break;
    }
  case 'q':
    {	
      zRot -= 1;
      break;
    }
  case 'Q':
    {	
      zRot -= 10;
      break;
    }
  case 'f':
    {	
      length += 1;
      break;
    }
  case 'd':
    {	
      length -= 1;
      break;
    }
  case 'F':
    {
      scale += .1f;
      break;
    }
  case 'D':
    {	
      scale -= .1f;
      break;
    }
  case 'h':
    { 	
      help=!help;
      break;
    }
  case 'j':
    { 
      hud=!hud;
      break;
    }
  default:
    {	
      break;
    }
  }
  switch (keyCode) {
  case UP:
    {	
      startY -= 10;
      break;
    }
  case DOWN:
    {	
      startY += 10;
      break;
    }
  case RIGHT:
    {
      startX += 10;
      break;
    }
  case LEFT:
    {	
      startX -= 10;
      break;
    }
  default: 
    {	
      break;
    }
  }

  if (key >= '0' && key <= '9') {
    loadRule((int)(key));
  }

  redraw();
}
public void mousePressed() {
  clickX = mouseX;
  clickY = mouseY;
}
public void mouseDragged() {
  if (keyPressed && keyCode==SHIFT) {
    length+=(clickY - mouseY)/5f;
    length = Math.abs(length);
  } 
  else {
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

public String wrap(String s, int maxLength) {
  StringBuffer wrapped = new StringBuffer();
  for (int i = 0; i*maxLength<s.length(); i++) {
    wrapped.append(s.substring(i*maxLength, min((s.length()), (i+1)*maxLength))+"\n");
  }
  return wrapped.substring(0, wrapped.length());
}


