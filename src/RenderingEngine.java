

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

import com.base.engine.core.CoreEngine;
import com.base.engine.core.Engine;
import com.base.engine.core.Time;
import com.base.engine.core.util.Util;
import com.base.engine.data.Mesh;
import com.base.engine.data.Resources;
import com.base.engine.physics.Frustum;
import com.base.engine.physics.Octree;
import com.base.engine.physics.PhysicsEngine;
import com.base.engine.rendering.Camera;
import com.base.engine.rendering.DirectionalLight;
import com.base.engine.rendering.GLFWWindow;
import com.base.engine.rendering.MaterialMap;
import com.base.engine.rendering.PointLight;
import com.base.engine.rendering.Projection;
import com.base.engine.rendering.SpotLight;
import com.base.engine.rendering.TContainer;
import com.base.engine.rendering.opengl.DeferredRenderer;
import com.base.engine.rendering.opengl.GLContext;
import com.base.engine.rendering.opengl.GLFramebuffer;
import com.base.engine.rendering.opengl.GLRendering;
import com.base.engine.rendering.opengl.GLShader;
import com.base.engine.rendering.opengl.GLTexture;
import com.base.engine.rendering.opengl.GLVertexArray;
import com.base.engine.rendering.opengl.Skybox;
import com.base.engine.rendering.opengl.Terrain;
import com.base.engine.rendering.water.WaterTile;

import math.MathUtil;
import math.Matrix4f;
import math.Quaternion;

import com.base.math.Transform;
import math.Vector3f;

public class RenderingEngine implements Engine {
	public static Camera camera;
	GLFWWindow window;
	GLContext context;
	Transform transform1 = new Transform();
	Mesh mesh, box;
	static int[] tex;
	Matrix4f ortho;
	Projection persp;
	MaterialMap material;
	DirectionalLight dlight;
	static DeferredRenderer drenderer;
	static Terrain terrain;
	
	public void start() {
		window = CoreEngine.window;
		context = new GLContext(33);
		context.viewport(window);
		window.setSizeCallback(context);
		
		glClearColor(0f, 1f, 1f, 1f);

		glFrontFace(GL_CCW);
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		
		glEnable(GL_DEPTH_TEST);

		//glEnable(GL_DEPTH_CLAMP);

		glEnable(GL_TEXTURE_2D); 
		
		tex = GLTexture.genTextures(1);
		GLTexture.createTextures(new String[]{"bricks.png"}, tex, 3);
		
		mesh = Resources.loadMesh("dragon.obj");
		
		box = Resources.loadMesh("Basic Cube.obj");
		//box = Resources.loadMesh("capsule.obj");
		
		Vector3f dlightpos = new Vector3f(-2f, 8f,-4f);
		float size = 20f;
		//ortho = Matrix4f.createPerspective(45f, 1f, -1.0f, 20f);
		ortho = new Matrix4f().orthographic(-size, size, -size, size, 1.0f, 20f);
		Matrix4f lightView = new Matrix4f().setLookAt(dlightpos);//new Matrix4f(new org.joml.Matrix4f().lookAt(2,10f,-10f,0,0,0,0,1,0));//Matrix4f.createLookAtMatrix(dlightpos, new Vector3f( 0.0f, 0.0f,  0.0f));
		ortho.mul(lightView);
		
		camera = new Camera();
		camera.translate(-5,0,5);
		//camera.lookAt(new Vector3f());
		//transform1.translate(0, 1,5);
		transform1.rotate(0, 45, 45);
		
		material = new MaterialMap(tex[0], 16f);
		
		dlight = new DirectionalLight(new Vector3f().sub(dlightpos), new Vector3f(0.3f,0.3f,0.3f), new Vector3f(0f,1f,1f), new Vector3f(1.0f, 1.0f, 1.0f));
		
		persp = new Projection(fov, window.getWidth(), window.getHeight(), .1f, 1000f);//Matrix4f.createPerspective(fov, aspectRatio, .1f, 1000f);
		
		terrain = new Terrain(100, 2, -0.5f,-0.5f, GLTexture.createTextures(new String[]{"bricks.png", "mud.png", "gauge.png", "grassTexture.png", "blendMap.png"}, 3));
		
		//org.joml.Matrix4f jm = new org.joml.Matrix4f().ortho(-400, 400, -300, 300, 0.1f, 1000f, false);//.orthographic(fov, window.getWidth() / window.getHeight(), .1f, 1000f);
	    drenderer = new DeferredRenderer(window.getWidth(), window.getHeight(), true, new Matrix4f().perspective(fov, window.getWidth() / window.getHeight(), .1f, 1000f));
		//drenderer = new DeferredRenderer(window.getWidth(), window.getHeight(), true, jm.get(Util.createFloatBuffer(16 * 4)));
	    drenderer.toggleShadows();
	    drenderer.toggleSSAO();
	    
	    proj = new Matrix4f().perspective(fov, window.getWidth() / window.getHeight(), .1f, 1000f);
	    view = new Matrix4f().setLookAt(new Vector3f(-10,4,-10f), new Vector3f());
	    
	    transforms = new TContainer(10);
	    
	    shader = Resources.loadShader("shader.glsl");
//	    Octree octree = new Octree(60,60,60);
//	    float step = 5;
//	    transforms = new TContainer(1000);
//	    for(int i = 0; i < 10; i++) for(int j = 0; j < 10; j++) for(int k = 0; k < 10; k++)
//	    	octree.add(new GameObject(step * i, step * j, step * k));
//	    octree.update(1.0f);
//	    
//	    Frustum f = new Frustum(camera, persp);
//	    
//	    octree.get(f, transforms);
	    
	    System.out.println(camera.getViewMatrix());
	    
	    Transform one = new Transform();
	    transforms.add(transform1);
	    
	    watershader = Resources.loadShader("water.glsl");
	    
	    water = new WaterTile(0, 0, 0, 80);
	    
	    Quaternion q = new Quaternion().rotate(0, 0, 180 * MathUtil.RAD);
	    
	    waterTransform = new Matrix4f().translationRotateScale(water.x, water.y, water.z, q.x, q.y, q.z, q.w, water.size, water.size, water.size);
	    
	    
		reflWidth = 320; reflHeight = 180;
		// Water Reflection
	    waterRefl = GLFramebuffer.genFramebuffers();
		
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, waterRefl);
		waterReflC = GLTexture.createColorbufferf(reflWidth, reflHeight);
		glBindTexture(GL_TEXTURE_2D, waterReflC);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, waterReflC, 0);
		  
		GL20.glDrawBuffers(GL_COLOR_ATTACHMENT0);
		
		waterReflD = GLFramebuffer.genRenderbuffer();
		GLFramebuffer.setUpDepthStencilBuffer(waterRefl, waterReflD, reflWidth, reflHeight);
	    
		refrWidth = window.getWidth(); refrHeight = window.getHeight();
		
		// Water Refraction
		waterRefr = GLFramebuffer.genFramebuffers();
		
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, waterRefr);
		waterRefrC = GLTexture.createColorbufferf(refrWidth, refrHeight);
		glBindTexture(GL_TEXTURE_2D, waterRefrC);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, waterRefrC, 0);
		  
		GL20.glDrawBuffers(GL_COLOR_ATTACHMENT0);
		
		waterRefrD = GLTexture.genTextures();
		glBindTexture(GL_TEXTURE_2D, waterRefrD);
		glTexImage2D(GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT32, refrWidth, refrHeight, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (ByteBuffer) null);
		GL11.glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, waterRefrD, 0);
		
		//THIS SKYBOX DOESN'T WORK!!!\\
		
		int skyboxTexture = GLTexture.createCubeMap(new String[] {"top.jpg", "bottom.jpg", "right.jpg", "left.jpg", "front.jpg", "back.jpg"});
		
		skybox = new Skybox(Resources.loadShader("skyboxshader.glsl"), skyboxTexture);
	    
		wave = GLTexture.createTexture("dudv.png");
	}
	int wave;
	Skybox skybox;
	int watershader;
	int shader;
	Matrix4f view;
	Matrix4f proj;
	TContainer transforms;
	static float fov = 45f, exposure = 1.0f;
	public void run() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
		
	    Matrix4f view = camera.getViewMatrix();
	    
	    
	    
//	    GLShader.bindProgram(shader);
//		GLShader.setUniformMat4(GL20.glGetUniformLocation(shader, "projection"), proj);
//		GLShader.setUniformMat4(GL20.glGetUniformLocation(shader, "view"), view);
//		GL13.glActiveTexture(GL13.GL_TEXTURE0);
//		GL11.glBindTexture(GL11.GL_TEXTURE_2D, material.diffuse);
		//GLShader.setUniformMat4(GL20.glGetUniformLocation(shader, "model"), PhysicsEngine.bodies[0].transform.getTransformation());
		
		GL11.glEnable(GL30.GL_CLIP_DISTANCE0);
	    GLShader.bindProgram(DeferredRenderer.TERRAIN_SHADER);
	    GLShader.setUniformVec4(DeferredRenderer.TERRAIN_SHADER, "clip_plane", 0, 0, 0, 0);
	    GLShader.bindProgram(DeferredRenderer.GBUFFER_SHADER);
	    GLShader.setUniformVec4(DeferredRenderer.GBUFFER_SHADER, "clip_plane", 0, 0, 0, 0);
	    renderWorld(view, camera.pos, dlight, 0, window.getWidth(), window.getHeight());
		//GLRendering.renderMesh(box.vao, box.indices);
		
		GLFramebuffer.copyDepthBuffer(drenderer.gdepth, 0, window.getWidth(), window.getHeight());
		
		//~~~~~~~~~RENDER REFRACTION~~~~~~~~~~~~\\
		
		GLShader.bindProgram(DeferredRenderer.TERRAIN_SHADER);
	    GLShader.setUniformVec4(DeferredRenderer.TERRAIN_SHADER, "clip_plane", 0, -1, 0, water.y);
	    GLShader.bindProgram(DeferredRenderer.GBUFFER_SHADER);
	    GLShader.setUniformVec4(DeferredRenderer.GBUFFER_SHADER, "clip_plane", 0, -1, 0, water.y);
		renderWorld(view, camera.pos, dlight, waterRefr, refrWidth, refrHeight);
	    
	    //~~~~~~~~~RENDER REFLECTION~~~~~~~~~~~~\\
	    
	    float distance = 2 * (camera.pos.y - water.y);
	    camera2.pos.set(camera.pos).y -= distance;
	    camera2.pitch = -camera.pitch;
	    camera2.yaw = camera.yaw;
	    camera2.hasRotated = true;
	    camera2.hasMoved = true;
	    view = camera2.getViewMatrix();
	    
	    GLShader.bindProgram(DeferredRenderer.TERRAIN_SHADER);
	    GLShader.setUniformVec4(DeferredRenderer.TERRAIN_SHADER, "clip_plane", 0, 1, 0, water.y);
	    GLShader.bindProgram(DeferredRenderer.GBUFFER_SHADER);
	    GLShader.setUniformVec4(DeferredRenderer.GBUFFER_SHADER, "clip_plane", 0, 1, 0, water.y);
		renderWorld(view, camera2.pos, dlight, waterRefl, reflWidth, reflHeight);
		
	    
	    view = camera.getViewMatrix();
		
		//System.out.println(PhysicsEngine.bodies[0].transform.getTransformation());
	    //drenderer.prepare(view);
	    //drenderer.render(mesh, count, material, transform1);
	    
	    //drenderer.render(box.vao, box.indices, transforms.next, material, transforms.transforms);
	    //drenderer.renderLighting(view, camera.pos, dlight, 0);
		
	    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	    GL11.glViewport(0, 0, window.getWidth(), window.getHeight());
	    GLShader.bindProgram(watershader);
		GLShader.setUniformMat4(GL20.glGetUniformLocation(watershader, "projection"), proj);
		GLShader.setUniformMat4(GL20.glGetUniformLocation(watershader, "view"), view);
		GLShader.setUniformMat4(GL20.glGetUniformLocation(watershader, "model"), waterTransform);
		GLShader.setUniform(watershader, "reflection", 0);
		GLShader.setUniform(watershader, "refraction", 1);
		GLShader.setUniform(watershader, "dudv", 2);
		move += wave_speed * Time.getDelta();
		move %= 1;
		GLShader.setUniform(watershader, "moveFactor", move);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, waterReflC);
		GL13.glActiveTexture(GL13.GL_TEXTURE1);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, waterRefrC);
		GL13.glActiveTexture(GL13.GL_TEXTURE2);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, wave);
		GLRendering.renderQuad();
		
		
		
		GLVertexArray.unbindVertexArray();
		window.swapBuffers();
		
		
		//System.out.println(" RenderingEngineDelta: " + Time.getDelta());
		
	}
	
	public void renderWorld(Matrix4f view, Vector3f camerapos, DirectionalLight dlight, int framebuffer, int width, int height) {
		drenderer.prepare(view);
		drenderer.render(box.vao, box.indices, material, HelloWorld.player.transform);
		for(int i = 0; i < HelloWorld.objects.length; i++) {
			GameObject object = HelloWorld.objects[i];
			if(object == null) continue;
			drenderer.render(object.mesh.vao, object.mesh.indices, object.material, object.transform);
		}
		
		drenderer.render(terrain);
		drenderer.renderLighting(view, camerapos, dlight, framebuffer, width, height);
		//skybox.render(view, framebuffer);
	}
	
	public static float move = 0;
	public static final float wave_speed = .2f;
	int[] dudv;
	int reflWidth, reflHeight, refrWidth, refrHeight;
	int waterRefl, waterReflC, waterReflD, waterRefr, waterRefrC, waterRefrD;
	Camera camera2 = new Camera();
	WaterTile water;
	Matrix4f waterTransform;
	
	public static void shadows(){drenderer.toggleShadows();}
	public static void ssao(){drenderer.toggleSSAO();}
	
static float moveSpeed = 15.0f;
	
	public static void movePlayerForward(){
		camera.translate(0, 0, -moveSpeed * Time.getDelta());
	}
	
	public static void movePlayerBackward(){
		camera.translate(0, 0, moveSpeed * Time.getDelta());
	}
	
	public static void movePlayerLeft(){
		camera.translate(-moveSpeed * Time.getDelta(), 0, 0);
	}
	
	public static void movePlayerRight(){
		camera.translate(moveSpeed * Time.getDelta(), 0, 0);
	}
	
}
