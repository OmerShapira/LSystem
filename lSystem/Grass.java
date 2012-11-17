package lSystem;


import java.util.Queue;

import processing.core.PApplet;
import processing.core.PVector;


//import toxi.physics2d.constraints.*;
//import toxi.physics.*;
//import toxi.physics.constraints.*;
//import toxi.physics2d.behaviors.*;
//import toxi.physics.behaviors.*;
//import toxi.physics2d.*;


public class Grass {

	int[] blade; 
	PApplet parent;
	float heightVar;
	boolean _DEBUG = false;
	/**
	 * 0 is only height, 1 is only wind
	 */
	float mixCoefficient = 0;


	public Grass(int maxVariation, int maxWidth, float maxHeightVar, PApplet parent) {
		this.parent = parent;
		heightVar = parent.random(1- maxHeightVar, 1 + maxHeightVar);
		blade = new int[4];
		blade[0] = (int) parent.random(0, maxWidth);
		blade[1] = 0;
		blade[2] = (int) parent.random(blade[0] -maxVariation, blade[0] + maxVariation);
		blade[3] = 1; //delta
	}

	
	
	public float[] getBlade(PVector wind, float heightFactor) {
		float midX = blade[2] + wind.x/2f;
		float midY = (blade[1] + blade[3]*heightFactor*heightVar + wind.y);
		float[] b = {
				blade[0], blade[1], 
				blade[0], midY*0.7f, blade[2]+ wind.x/3f, midY*0.99f, 
				blade[2]+ wind.x/3f, blade[1]+blade[3]*heightFactor*heightVar +wind.y
		};
		return b;
	}
	
	
	/**
	 * Deprecated
	 * @param heightFactor
	 */
	public void render(float heightFactor){

		float [] blade = getBlade(new PVector(parent.noise(parent.frameCount/20f)*30,0), heightFactor);
		parent.bezier(blade[0],blade[1],blade[2],blade[3],blade[4],blade[5],blade[6],blade[7]);
	}

	
	
	public void feedToQueue(Queue<Float> q, float heightFactor, int segments, int width, int height){
		
		float [] blade = getBlade(new PVector(parent.noise(parent.frameCount/20f)*30,0), heightFactor);
		
		for (int i = 0; i < segments-1 ; i++){
			// Get point and get next point
			float t = i*1f/segments;
			float r = (i+1f)/segments;
			
			// Add a line (XYXY) into the vertex array //TODO find a way to 
			q.add(width  - 	parent.bezierPoint(blade[0],blade[2],blade[4],blade[6],t)); //X1
			q.add(height - 	parent.bezierPoint(blade[1],blade[3],blade[5],blade[7],t)); //Y1
			q.add(width  - 	parent.bezierPoint(blade[0],blade[2],blade[4],blade[6],r));	//X2
			q.add(height - 	parent.bezierPoint(blade[1],blade[3],blade[5],blade[7],r)); //Y2

			//Show vertices
			if (_DEBUG){
			parent.print(parent.bezierPoint(blade[0],blade[2],blade[4],blade[6],t));
			parent.println(" , "+parent.bezierPoint(blade[1],blade[3],blade[5],blade[7],t));
			}
		}
	}
}








