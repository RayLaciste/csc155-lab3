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
	private int vbo[] = new int[11];
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
	float[] lightAmbient = new float[] { 0.0f, 1.0f, 0.0f, 1.0f };
	float[] lightDiffuse = new float[] { 0.0f, 1.0f, 0.0f, 1.0f };
	float[] lightSpecular = new float[] { 0.0f, 1.0f, 0.0f, 1.0f };
		
	// gold material
	float[] matAmb = Utils.goldAmbient();
	float[] matDif = Utils.goldDiffuse();
	float[] matSpe = Utils.goldSpecular();
	float matShi = Utils.goldShininess();
	// ----------------------  ----------------------

	// ---------------------- Models and Textures ----------------------
	private int ufoTexture;
	private int cowTexture;
	private int groundTexture;
	private int axisTexture;

	private int numObjVerticesUfo, numObjVerticesCow;
	private ImportedModel ufoModel, cowModel;

	// Axes
	private boolean visibleAxis = true;
	private float axesX = 0.0f;

	// ufo
	private float ufoPositionY = 1.5f;
	private float ufoPositionZ = 0.0f;
	private float ufoMovementSpeed = 0.5f;
	private float ufoWave = .75f;  // How wide the sine wave is

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
//		lightRotationAngle = (float)(elapsedTime * 0.05);
//		currentLightPos.set(initialLightLoc);
//		currentLightPos.rotateAxis((float)Math.toRadians(lightRotationAngle), 0.0f, 0.0f, 1.0f);
		currentLightPos.set(0.0f, ufoPositionY - 0.1f, ufoPositionZ);

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

		// ---------------------- Ufo ----------------------
		mvStack.pushMatrix();

//		ufoPositionZ = (float) Math.sin(elapsedTime * 0.001f * ufoMovementSpeed) * ufoWave;

		ufoPositionZ = 0;

		mvStack.translate(0, ufoPositionY, ufoPositionZ)
				.scale(0.5f)
				.rotateY((float) Math.toRadians(90.0f));

		mMat.set(mvStack);
		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		// Send matrices to shaders
		gl.glUniformMatrix4fv(mLoc, 1, false, mvStack.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		// Ufo Vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// Ufo Texture
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		// Ufo normals
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		// Binding Texture
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, ufoTexture);

		// Render
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, ufoModel.getNumVertices());
		mvStack.popMatrix();

		// ---------------------- Cow ----------------------
		mvStack.pushMatrix();

		mvStack.translate(0, 0, 0).scale(0.05f).rotateY((float) Math.toRadians(-90.0f));

		mMat.set(mvStack);
		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		// Send matrices to shaders
		gl.glUniformMatrix4fv(mLoc, 1, false, mvStack.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		// Cow Vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// Cow Texture
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		// Cow normals
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		// Binding Texture
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, cowTexture);

		// Render
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, cowModel.getNumVertices());
		mvStack.popMatrix();

		// --------------------------------------------
		mvStack.popMatrix();

	}

	public void init(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();

		ufoModel = new ImportedModel("ufo.obj");
		cowModel = new ImportedModel("cow.obj");

		renderingProgram = Utils.createShaderProgram("a3/vertShader.glsl", "a3/fragShader.glsl");

		startTime = System.currentTimeMillis();
		prevTime = startTime;

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		setupVertices();

		cameraX = 0.0f;
		cameraY = 1.5f;
		cameraZ = 5.0f;

		ufoTexture = Utils.loadTexture("ufo.png");
		cowTexture = Utils.loadTexture("cow.png");
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

		// ---------------------- Ufo ----------------------
		numObjVerticesUfo = ufoModel.getNumVertices();
		Vector3f[] ufovertices = ufoModel.getVertices();
		Vector2f[] ufotexCoords = ufoModel.getTexCoords();
		Vector3f[] ufonormals = ufoModel.getNormals();

		float[] pvaluesufo = new float[numObjVerticesUfo * 3];
		float[] tvaluesufo = new float[numObjVerticesUfo * 2];
		float[] nvaluesufo = new float[numObjVerticesUfo * 3];

		for (int i = 0; i < numObjVerticesUfo; i++) {
			pvaluesufo[i * 3] = (float) (ufovertices[i]).x();
			pvaluesufo[i * 3 + 1] = (float) (ufovertices[i]).y();
			pvaluesufo[i * 3 + 2] = (float) (ufovertices[i]).z();
			tvaluesufo[i * 2] = (float) (ufotexCoords[i]).x();
			tvaluesufo[i * 2 + 1] = (float) (ufotexCoords[i]).y();
			nvaluesufo[i * 3] = (float) (ufonormals[i]).x();
			nvaluesufo[i * 3 + 1] = (float) (ufonormals[i]).y();
			nvaluesufo[i * 3 + 2] = (float) (ufonormals[i]).z();
		}

		// ---------------------- Car ----------------------
		numObjVerticesCow = cowModel.getNumVertices();
		Vector3f[] cowvertices = cowModel.getVertices();
		Vector2f[] cowtexCoords = cowModel.getTexCoords();
		Vector3f[] cownormals = cowModel.getNormals();

		float[] pvaluescow = new float[numObjVerticesCow * 3];
		float[] tvaluescow = new float[numObjVerticesCow * 2];
		float[] nvaluescow = new float[numObjVerticesCow * 3];

		for (int i = 0; i < numObjVerticesCow; i++) {
			pvaluescow[i * 3] = (float) (cowvertices[i]).x();
			pvaluescow[i * 3 + 1] = (float) (cowvertices[i]).y();
			pvaluescow[i * 3 + 2] = (float) (cowvertices[i]).z();
			tvaluescow[i * 2] = (float) (cowtexCoords[i]).x();
			tvaluescow[i * 2 + 1] = (float) (cowtexCoords[i]).y();
			nvaluescow[i * 3] = (float) (cownormals[i]).x();
			nvaluescow[i * 3 + 1] = (float) (cownormals[i]).y();
			nvaluescow[i * 3 + 2] = (float) (cownormals[i]).z();
		}

		// --------------------------------------------
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);

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

		// ---------------------- Ufo ----------------------
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvaluesufo);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvaluesufo);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit() * 4, texBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvaluesufo);
		gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit() * 4, norBuf, GL_STATIC_DRAW);

		// ---------------------- Cow ----------------------
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		FloatBuffer vertBufCow = Buffers.newDirectFloatBuffer(pvaluescow);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBufCow.limit() * 4, vertBufCow, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		FloatBuffer texBufCow = Buffers.newDirectFloatBuffer(tvaluescow);
		gl.glBufferData(GL_ARRAY_BUFFER, texBufCow.limit() * 4, texBufCow, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		FloatBuffer norBufCow = Buffers.newDirectFloatBuffer(nvaluescow);
		gl.glBufferData(GL_ARRAY_BUFFER, norBufCow.limit() * 4, norBufCow, GL_STATIC_DRAW);


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