package a3;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.nio.*;
import javax.swing.*;
import java.lang.Math;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.common.nio.Buffers;
import org.joml.*;

public class Code extends JFrame implements GLEventListener, KeyListener
{	private GLCanvas myCanvas;
	private int renderingProgram;
	private int vao[] = new int[1];
	private int vbo[] = new int[8];
	private Vector3f cameraLoc = new Vector3f(0,0,1.5f);

	// ---------------------- Camera ----------------------
	private float cameraPitch = 0.0f;
	private float cameraYaw = 0.0f;
	private float cameraX, cameraY, cameraZ;

	// ---------------------- TIME ----------------------
	private double startTime = 0;
	private double prevTime = 0;
	private double deltaTime = 0;
	private double elapsedTime = 0;
	private double currentTime = 0;
	// ----------------------  ----------------------
	
	// allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private Matrix4f pMat = new Matrix4f();  // perspective matrix
	private Matrix4f vMat = new Matrix4f();  // view matrix
	private Matrix4f mMat = new Matrix4f();  // model matrix
	private Matrix4f invTrMat = new Matrix4f(); // inverse-transpose
	private int mLoc, vLoc, pLoc, nLoc, sampLoc;
	private int globalAmbLoc, ambLoc, diffLoc, specLoc, posLoc, mambLoc, mdiffLoc, mspecLoc, mshiLoc;
	private float aspect;
	private Vector3f currentLightPos = new Vector3f();
	private float[] lightPos = new float[3];

	// ---------------------- LIGHTING ----------------------
	private Vector3f initialLightLoc = new Vector3f(5.0f, 2.0f, 2.0f);

	float lightRotationAngle = 0;

	// white light properties
	float[] globalAmbient = new float[] { 0.6f, 0.6f, 0.6f, 1.0f };
	float[] lightAmbient = new float[] { 0.1f, 0.1f, 0.1f, 1.0f };
	float[] lightDiffuse = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
	float[] lightSpecular = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
		
	// gold material
	float[] matAmb = Utils.goldAmbient();
	float[] matDif = Utils.goldDiffuse();
	float[] matSpe = Utils.goldSpecular();
	float matShi = Utils.goldShininess();
	// ----------------------  ----------------------

	// ---------------------- Models and Textures ----------------------
	private int carTexture;
	private int groundTexture;
	private int axisTexture;

	private int numObjVertices;
	private ImportedModel myModel;

	// Torus
	private Torus myTorus;
	private int numTorusVertices, numTorusIndices;

	// Axes
	private boolean visibleAxis = true;
	private float axesX = 0.0f;

	// ----------------------  ----------------------

	private Matrix4fStack mvStack = new Matrix4fStack(16);


	public Code()
	{	setTitle("Lab 3");
		setSize(800, 800);
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		myCanvas.addKeyListener(this);
		this.add(myCanvas);
		this.setVisible(true);
		Animator animator = new Animator(myCanvas);
		animator.start();
	}

	public void display(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClear(GL_DEPTH_BUFFER_BIT);

		gl.glUseProgram(renderingProgram);

		// Uniform Variables
		mLoc = gl.glGetUniformLocation(renderingProgram, "m_matrix");
		vLoc = gl.glGetUniformLocation(renderingProgram, "v_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgram, "p_matrix");
		nLoc = gl.glGetUniformLocation(renderingProgram, "norm_matrix");
		sampLoc = gl.glGetUniformLocation(renderingProgram, "samp");

//		Old Torus Code
//		mMat.translation(torusLoc.x(), torusLoc.y(), torusLoc.z());
//		mMat.rotateX((float)Math.toRadians(35.0f));

		// Time Values
		currentTime = System.currentTimeMillis();
		elapsedTime = currentTime - startTime;
		deltaTime = (currentTime - prevTime) / 1000;
		prevTime = currentTime;

		// Light Position
		lightRotationAngle = (float)(elapsedTime * 0.1);
		currentLightPos.set(initialLightLoc);
		currentLightPos.rotateAxis((float)Math.toRadians(lightRotationAngle), 0.0f, 0.0f, 1.0f);

		// Camera stuff
		vMat.identity();
		vMat.translation(-cameraX, -cameraY, -cameraZ);

		// Pass perspective matrix to shader
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));

		mvStack.pushMatrix();

		// update lights
		installLights();

		// ---------------------- Ground ----------------------
		mvStack.pushMatrix();
		mvStack.translate(0.0f, 0.0f, 0.0f);

		mMat.set(mvStack);
		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		// Send matrices to shaders
		gl.glUniformMatrix4fv(mLoc, 1, false, mvStack.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		// Ground Vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// Ground Textures
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		// Ground normals
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		// Binding Texture
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, groundTexture);

		gl.glUniform1i(sampLoc, 0);


		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, 6);

		mvStack.popMatrix();

		// ---------------------- Car ----------------------
		mvStack.pushMatrix();
		mvStack.translate(0.0f, 0.175f, 0.0f).scale(0.0015f).rotateY((float) Math.toRadians(90.0f));

		mMat.set(mvStack);
		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		// Send matrices to shaders
		gl.glUniformMatrix4fv(mLoc, 1, false, mvStack.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		// Car Vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// Car Texture
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		// Car normals
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		// Binding Texture
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, carTexture);

		// Render
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, myModel.getNumVertices());
		mvStack.popMatrix();

		// --------------------------------------------
		mvStack.popMatrix();

// 		Torus Old Code
//		gl.glEnable(GL_CULL_FACE);
//		gl.glFrontFace(GL_CCW);
//		gl.glEnable(GL_DEPTH_TEST);
//		gl.glDepthFunc(GL_LEQUAL);

//		gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[3]);
//		gl.glDrawElements(GL_TRIANGLES, numTorusIndices, GL_UNSIGNED_INT, 0);
	}

	public void init(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		myModel = new ImportedModel("car.obj");

		renderingProgram = Utils.createShaderProgram("a3/vertShader.glsl", "a3/fragShader.glsl");

		startTime = System.currentTimeMillis();
		prevTime = startTime;

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		setupVertices();

		cameraX = 0.0f;
		cameraY = 2.0f;
		cameraZ = 10.0f;

		carTexture = Utils.loadTexture("car.png");
		groundTexture = Utils.loadTexture("ground.jpg");
		axisTexture = Utils.loadTexture("axis.png");

		gl.glBindTexture(GL_TEXTURE_2D, groundTexture);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
	}
	
	private void installLights()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		
		lightPos[0]=currentLightPos.x(); lightPos[1]=currentLightPos.y(); lightPos[2]=currentLightPos.z();
		
		// get the locations of the light and material fields in the shader
		globalAmbLoc = gl.glGetUniformLocation(renderingProgram, "globalAmbient");
		ambLoc = gl.glGetUniformLocation(renderingProgram, "light.ambient");
		diffLoc = gl.glGetUniformLocation(renderingProgram, "light.diffuse");
		specLoc = gl.glGetUniformLocation(renderingProgram, "light.specular");
		posLoc = gl.glGetUniformLocation(renderingProgram, "light.position");
		mambLoc = gl.glGetUniformLocation(renderingProgram, "material.ambient");
		mdiffLoc = gl.glGetUniformLocation(renderingProgram, "material.diffuse");
		mspecLoc = gl.glGetUniformLocation(renderingProgram, "material.specular");
		mshiLoc = gl.glGetUniformLocation(renderingProgram, "material.shininess");
	
		//  set the uniform light and material values in the shader
		gl.glProgramUniform4fv(renderingProgram, globalAmbLoc, 1, globalAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram, ambLoc, 1, lightAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram, diffLoc, 1, lightDiffuse, 0);
		gl.glProgramUniform4fv(renderingProgram, specLoc, 1, lightSpecular, 0);
		gl.glProgramUniform3fv(renderingProgram, posLoc, 1, lightPos, 0);
		gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, matAmb, 0);
		gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, matDif, 0);
		gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, matSpe, 0);
		gl.glProgramUniform1f(renderingProgram, mshiLoc, matShi);
	}

	private void setupVertices()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();

		// ---------------------- Torus ----------------------
//		myTorus = new Torus(0.5f, 0.2f, 48);
//		numTorusVertices = myTorus.getNumVertices();
//		numTorusIndices = myTorus.getNumIndices();
//
//		Vector3f[] vertices = myTorus.getVertices();
//		Vector2f[] texCoords = myTorus.getTexCoords();
//		Vector3f[] normals = myTorus.getNormals();
//		int[] indices = myTorus.getIndices();
//
//		float[] pvalues = new float[vertices.length*3];
//		float[] tvalues = new float[texCoords.length*2];
//		float[] nvalues = new float[normals.length*3];
//
//		for (int i=0; i<numTorusVertices; i++)
//		{	pvalues[i*3]   = (float) vertices[i].x();
//			pvalues[i*3+1] = (float) vertices[i].y();
//			pvalues[i*3+2] = (float) vertices[i].z();
//			tvalues[i*2]   = (float) texCoords[i].x();
//			tvalues[i*2+1] = (float) texCoords[i].y();
//			nvalues[i*3]   = (float) normals[i].x();
//			nvalues[i*3+1] = (float) normals[i].y();
//			nvalues[i*3+2] = (float) normals[i].z();
//		}

		// ---------------------- Axis Lines ----------------------
		float[] axisVertices = {
				// X
				0.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,

				// Y
				0.0f, 0.0f, 0.0f,
				0.0f, 1.0f, 0.0f,

				// Z
				0.0f, 0.0f, 0.0f,
				0.0f, 0.0f, 1.0f
		};

		float[] axisTexCoords = {
				// X
				1.0f, 0.0f,
				1.0f, 0.0f,

				// Y
				0.0f, 1.0f,
				0.0f, 1.0f,

				// Z
				0.0f, 0.0f,
				0.0f, 0.0f
		};

		// ---------------------- Ground ----------------------
		float[] groundVertices =
				{
						-5.0f, 0.0f, -5.0f,  // Bottom-left
						5.0f, 0.0f, -5.0f,  // Bottom-right
						-5.0f, 0.0f,  5.0f,  // Top-left
						5.0f, 0.0f, -5.0f,  // Bottom-right
						5.0f, 0.0f,  5.0f,  // Top-right
						-5.0f, 0.0f,  5.0f   // Top-left
				};

		float[] groundTexCoords =
				{
						0.0f, 0.0f,  // Bottom-left
						5.0f, 0.0f,  // Bottom-right
						0.0f, 5.0f,  // Top-left
						5.0f, 0.0f,  // Bottom-right
						5.0f, 5.0f,  // Top-right
						0.0f, 5.0f   // Top-left
				};

		float[] groundNormals =
				{
						0.0f, 1.0f, 0.0f,
						0.0f, 1.0f, 0.0f,
						0.0f, 1.0f, 0.0f,
						0.0f, 1.0f, 0.0f,
						0.0f, 1.0f, 0.0f,
						0.0f, 1.0f, 0.0f
				};

		// ---------------------- Car ----------------------
		numObjVertices = myModel.getNumVertices();
		Vector3f[] vertices = myModel.getVertices();
		Vector2f[] texCoords = myModel.getTexCoords();
		Vector3f[] normals = myModel.getNormals();

		float[] pvalues = new float[numObjVertices * 3];
		float[] tvalues = new float[numObjVertices * 2];
		float[] nvalues = new float[numObjVertices * 3];

		for (int i = 0; i < numObjVertices; i++) {
			pvalues[i * 3] = (float) (vertices[i]).x();
			pvalues[i * 3 + 1] = (float) (vertices[i]).y();
			pvalues[i * 3 + 2] = (float) (vertices[i]).z();
			tvalues[i * 2] = (float) (texCoords[i]).x();
			tvalues[i * 2 + 1] = (float) (texCoords[i]).y();
			nvalues[i * 3] = (float) (normals[i]).x();
			nvalues[i * 3 + 1] = (float) (normals[i]).y();
			nvalues[i * 3 + 2] = (float) (normals[i]).z();
		}

		// --------------------------------------------
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);

		// ---------------------- Torus ----------------------
//		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
//		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
//		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);
//
//		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
//		FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
//		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit()*4, texBuf, GL_STATIC_DRAW);
//
//		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
//		FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
//		gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit()*4, norBuf, GL_STATIC_DRAW);
//
//		gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[3]);
//		IntBuffer idxBuf = Buffers.newDirectIntBuffer(indices);
//		gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxBuf.limit()*4, idxBuf, GL_STATIC_DRAW);

		// ---------------------- Ground ----------------------
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer groundBuf = Buffers.newDirectFloatBuffer(groundVertices);
		gl.glBufferData(GL_ARRAY_BUFFER, groundBuf.limit() * 4, groundBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer groundTex = Buffers.newDirectFloatBuffer(groundTexCoords);
		gl.glBufferData(GL_ARRAY_BUFFER, groundTex.limit() * 4, groundTex, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);  // Use an unused VBO slot
		FloatBuffer groundNor = Buffers.newDirectFloatBuffer(groundNormals);
		gl.glBufferData(GL_ARRAY_BUFFER, groundNor.limit() * 4, groundNor, GL_STATIC_DRAW);

		// ---------------------- Axis Lines ----------------------
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer axisVertBuf = Buffers.newDirectFloatBuffer(axisVertices);
		gl.glBufferData(GL_ARRAY_BUFFER, axisVertBuf.limit() * 4, axisVertBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		FloatBuffer axisTexBuf = Buffers.newDirectFloatBuffer(axisTexCoords);
		gl.glBufferData(GL_ARRAY_BUFFER, axisTexBuf.limit() * 4, axisTexBuf, GL_STATIC_DRAW);

		// ---------------------- Car ----------------------
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit() * 4, texBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit() * 4, norBuf, GL_STATIC_DRAW);


	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public void keyPressed(KeyEvent e) {
		float speed = 5f;
		float cameraSpeed = 0.05f;
		switch (e.getKeyCode()) {
			case KeyEvent.VK_W:
				cameraZ -= (float)(speed * deltaTime);
				break;
			case KeyEvent.VK_A:
				cameraX -= (float)(speed * deltaTime);
				break;
			case KeyEvent.VK_S:
				cameraZ += (float)(speed * deltaTime);
				break;
			case KeyEvent.VK_D:
				cameraX += (float)(speed * deltaTime);
				break;
			case KeyEvent.VK_UP:
				cameraPitch -= cameraSpeed;
				break;
			case KeyEvent.VK_DOWN:
				cameraPitch += cameraSpeed;
				break;
			case KeyEvent.VK_LEFT:
				cameraYaw -= cameraSpeed;
				break;
			case KeyEvent.VK_RIGHT:
				cameraYaw += cameraSpeed;
				break;
			case KeyEvent.VK_SPACE:
				if (visibleAxis) {
					axesX += 10f;
				} else {
					axesX -= 10f;
				}
				visibleAxis = !visibleAxis;
				break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {

	}

	public static void main(String[] args) { new Code(); }
	public void dispose(GLAutoDrawable drawable) {}
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
	{	aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);
	}

}