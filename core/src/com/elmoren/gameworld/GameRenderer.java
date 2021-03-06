package com.elmoren.gameworld;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.decals.GroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.SimpleOrthoGroupStrategy;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.elmoren.cchelpers.AssetLoader;
import com.elmoren.gameobjects.Cat;
import com.elmoren.gameobjects.Scrollable;
import com.elmoren.gameobjects.Tree;

public class GameRenderer {

	private GameWorld world;
    private PerspectiveCamera cam;
    private ShapeRenderer shapeRenderer;

    private CameraGroupStrategy groupStrategy;
    private DecalBatch spriteBatch;
    private SpriteBatch batcher;

    private int midPointY;
    private int gameHeight;
    private int gameWidth;

    private Cat cat;
    private float lastRotation;

    private ArrayList<Tree> obstacles;

	public GameRenderer(GameWorld world, int gameWidth, int gameHeight, int midPointY) {

		this.world = world;
		this.gameWidth = gameWidth;
        this.gameHeight = gameHeight;
        this.midPointY = midPointY;

		this.cam = new PerspectiveCamera(80, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

	    cam.rotate(180, 0, 0, 1);
		cam.position.set(0f, 400f, -100f);
	    cam.near = 0.1f;
	    cam.far = 800f;
	    cam.lookAt(0,0,0);
	    cam.normalizeUp();
	    cam.update();

        groupStrategy = new CameraGroupStrategy(cam);

        spriteBatch = new DecalBatch(groupStrategy);
	    batcher = new SpriteBatch();
	    batcher.setProjectionMatrix(cam.combined);

        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setProjectionMatrix(cam.combined);

        lastRotation = 0f;

        initAssets();
	}

	public void initAssets() {
		this.cat = world.getCat();
		this.obstacles = world.getObstacles();
	}

	public void render(float runTime) {
        float wOffset;
        float vOffset;
		Cat cat = world.getCat();

        Gdx.gl.glClearColor(world.getSkyColor().r,
                world.getSkyColor().g,
                world.getSkyColor().b,
                world.getSkyColor().a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        this.cam.rotate((lastRotation - cat.getRotation()) / 3.0f, 0, 1, 0);
        cam.update();
        shapeRenderer.setProjectionMatrix(cam.combined);

        lastRotation = cat.getRotation();

        // Tells the shapeRenderer to finish rendering
        // We MUST do this every time.
        shapeRenderer.begin(ShapeType.Filled);
        Rectangle r = world.getGround();
        shapeRenderer.setColor(1 / 255.0f, 167 / 255.0f, 17 / 255.0f, 1);
        shapeRenderer.rect(r.x, r.y, r.getWidth(), r.getHeight());
        shapeRenderer.end();

        shapeRenderer.begin(ShapeType.Filled);
		Rectangle r1 = cat.getBoundingBox();
		shapeRenderer.setColor(world.getGroundColor());
		//shapeRenderer.rect(r1.x, r1.y, r1.getWidth(), r1.getHeight());
		shapeRenderer.end();

        // Set Cat Rotation and Eleveation
        // Cat animation time
        AssetLoader.stateTime += Gdx.graphics.getDeltaTime();

        if (!cat.isAlive() && AssetLoader.catDecal.getZ() < 50f) {
            // Make CatCoptor Fall if dead
            AssetLoader.catDecal.setZ(AssetLoader.catDecal.getZ() + 7.5f);
        }
        else if (cat.isAlive()) {
            TextureRegion currentFrame = AssetLoader.catAnimation.getKeyFrame(AssetLoader.stateTime, true);
            AssetLoader.catDecal = Decal.newDecal(currentFrame, true);
            AssetLoader.catDecal.setRotation(cam.direction, cam.up);
            AssetLoader.catDecal.rotateZ(cat.getRotation() / 2.0f);
            AssetLoader.catDecal.setY(cat.getY());
            AssetLoader.catDecal.setZ(cat.getElevation() * -1.0f);
        }
        spriteBatch.add(AssetLoader.catDecal);

        shapeRenderer.begin(ShapeType.Filled);
        shapeRenderer.setColor(160 / 255.0f, 15 / 255.0f, 17 / 255.0f, 1);

        // TODO: 6/21/2016 Add the sun and sky sprites later
        // AssetLoader.sunDecal.setPosition(Gdx.graphics.getWidth()*0.7f, -275, -230);
		// AssetLoader.sunDecal.setScale(8);
        // spriteBatch.add(AssetLoader.sunDecal);

        int i = 0;
        for (Scrollable s : obstacles) {
        	//shapeRenderer.rect(s.getX() - (s.getWidth()/2), s.getY(), s.getWidth(), s.getHeight() );
        	AssetLoader.treeDecals[i].setPosition(s.getX(), s.getY(), -1f);
            spriteBatch.add(AssetLoader.treeDecals[i]);
            i++;
        }

        spriteBatch.flush();
        shapeRenderer.end();
        String score = world.getScore() + "";

                    // Change Text shown based on game state!
        Matrix4 normalProjection = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(),  Gdx.graphics.getHeight());
        batcher.setProjectionMatrix(normalProjection);
        batcher.begin();

        if (world.isReady()) {
            AssetLoader.glyphLayout.setText(AssetLoader.font, "Touch to Start");
            wOffset = (Gdx.graphics.getWidth() - AssetLoader.glyphLayout.width) / 2;
            vOffset =  Gdx.graphics.getHeight() / 2 + AssetLoader.glyphLayout.height / 2;
            AssetLoader.shadow.draw(batcher, AssetLoader.glyphLayout, wOffset, vOffset - 1);
            AssetLoader.font.draw(batcher, AssetLoader.glyphLayout, wOffset, vOffset);
        }
        else {

            if (world.isGameOver() || world.isHighScore()) {

                if (world.isGameOver()) {
                    AssetLoader.glyphLayout.setText(AssetLoader.font, "Game Over");
                    wOffset = (Gdx.graphics.getWidth() - AssetLoader.glyphLayout.width) / 2;
                    vOffset = (Gdx.graphics.getHeight() / 5)*4; // - (AssetLoader.glyphLayout.height / 2);
                    AssetLoader.shadow.draw(batcher, AssetLoader.glyphLayout, wOffset, vOffset-1);
                    AssetLoader.font.draw(batcher, AssetLoader.glyphLayout, wOffset, vOffset);

                    AssetLoader.glyphLayout.setText(AssetLoader.font, "High Score:");
                    wOffset = (Gdx.graphics.getWidth() - AssetLoader.glyphLayout.width) / 2;
                    vOffset = vOffset - (Gdx.graphics.getHeight()/5);
        			AssetLoader.shadow.draw(batcher, AssetLoader.glyphLayout, wOffset, vOffset-1);
        			AssetLoader.font.draw(batcher, AssetLoader.glyphLayout, wOffset, vOffset);

        			String highScore = AssetLoader.getHighScore() + "";
                    AssetLoader.glyphLayout.setText(AssetLoader.font, highScore);
                    wOffset = (Gdx.graphics.getWidth() - AssetLoader.glyphLayout.width) / 2;
                    vOffset = vOffset - (Gdx.graphics.getHeight()/5);
                    AssetLoader.shadow.draw(batcher, AssetLoader.glyphLayout, wOffset, vOffset - 1);
        	        AssetLoader.font.draw(batcher, AssetLoader.glyphLayout, wOffset, vOffset);
        		}
        		else {
        			// High Score!
                    AssetLoader.glyphLayout.setText(AssetLoader.font, "High Score!");
                    wOffset = (Gdx.graphics.getWidth() - AssetLoader.glyphLayout.width) / 2;
                    vOffset = Gdx.graphics.getHeight() / 2 + (AssetLoader.glyphLayout.height / 2);
                    AssetLoader.shadow.draw(batcher, AssetLoader.glyphLayout, wOffset, vOffset-1);
                    AssetLoader.font.draw(batcher, AssetLoader.glyphLayout, wOffset,vOffset);
        		}

                AssetLoader.glyphLayout.setText(AssetLoader.font, "Try Again?");
                wOffset = (Gdx.graphics.getWidth() - AssetLoader.glyphLayout.width) / 2;
                vOffset = Gdx.graphics.getHeight() / 5 + (AssetLoader.glyphLayout.height / 3);
                AssetLoader.shadow.draw(batcher, AssetLoader.glyphLayout, wOffset, vOffset - 1);
                AssetLoader.font.draw(batcher, AssetLoader.glyphLayout, wOffset, vOffset);
        	}

        	// Current score always visible when not in READY state
            AssetLoader.glyphLayout.setText(AssetLoader.font, score);
            wOffset = (Gdx.graphics.getWidth() - AssetLoader.glyphLayout.width) / 2;
            AssetLoader.shadow.draw(batcher, AssetLoader.glyphLayout, wOffset,
        			Gdx.graphics.getHeight() - 11 );
        	AssetLoader.font.draw(batcher, AssetLoader.glyphLayout, wOffset,
        			Gdx.graphics.getHeight() - 10);

        }
        batcher.end();
	}

}
