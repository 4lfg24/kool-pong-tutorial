package template

import de.fabmax.kool.KoolContext
import de.fabmax.kool.input.KeyboardInput
import de.fabmax.kool.math.Mat4f
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.physics.*
import de.fabmax.kool.physics.geometry.BoxGeometry
import de.fabmax.kool.physics.geometry.SphereGeometry
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.colorMesh
import de.fabmax.kool.scene.scene
import de.fabmax.kool.util.*
import pongTutorial.PongPaddle
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * Main application entry. This demo creates a small example scene, which you probably want to replace by your actual
 * game / application content.
 */
//physics world variables, these will be responsible for
//handling physics and collision detection
lateinit var world: PhysicsWorld
var stepper = ConstantPhysicsStepperSync()
lateinit var ball: RigidDynamic

//sizes for the walls delimiting the game area
var wallDimension = 100f
//the pong paddles that the players will control
lateinit var paddle1: PongPaddle
lateinit var paddle2: PongPaddle

//font size (for the ui)
var fontSize = 45f
//this variables will hold the score of each player
var playerScore1 = 0
var playerScore2 = 0

//timer to respawn the ball after a player scores a point
var respawnTimer = 1.5f
var respawnBall = false

fun pong(ktx: KoolContext) {
    //initializing the main scene, it will contain our graphics (models) and
    //physic objects
    ktx.scenes += scene {
        //we set the camera far away from the center so that the game area
        // will look smaller
        camera.apply {
            position.set(0f, 0f, 150f)
            clipNear = 0.5f
            clipFar = 500f
            lookAt.set(Vec3f(0f, 0f, 0f))
        }
        //remember that the physics world must be initialized
        //first, or we will get errors when trying to add actors to it
        loadPhysicsWorld()

        loadPaddles()
        loadWalls()
        loadBall()

        //setting up basic lighting for the scene, lighting is a complex
        //topic, might do a tutorial on it another time
        lighting.singleLight {
            setDirectional(Vec3f(-1f, -1f, -1f))
            setColor(Color.WHITE, 4f)
        }
        //on update is a function called every frame, here we can put the logic
        //for the game, in this case checking the ball's position to see if a player
        //scored and updating the paddles
        onUpdate {
            checkBallPosition()
            paddle1.update()
            paddle2.update()
        }
    }

    //now we add another scene, this time dedicated to the UI, which will display
    //the player's respective score
    ktx.scenes += UiScene {
        //IMPORTANT: always call this function when initializing a
        //Ui scene, necessary for rendering the ui properly
        setupUiScene()
        //The Panel function creates a UISurface, which can be used to host
        //different ui elements
        Panel(colors = Colors.singleColorDark(MdColor.BLUE)) {
            modifier
                .size(300.dp, 100.dp)
                .align(AlignmentX.Center, AlignmentY.Top)
                .margin(top = 40.dp) //how distant from the top of its alignment is
                .padding(top = 40.dp) //how much everything inside it is distant from the top

            //we initialize a Msdf font, which allows for great personalized
            //text, we will use this to draw the score
            val font = MsdfFont(
                sizePts = fontSize,
                glowColor = colors.secondary.withAlpha(0.5f)
            )
            //we initialize two mutable state value, these are used
            //specifically inside a Ui context and are useful because every
            //Ui element that uses them will be uploaded as soon as they change
            var player1Text by remember(0)
            var player2Text by remember(0)

            //we create a text field, which will hold the score of the players
            Text("$player1Text - $player2Text") {
                modifier.backgroundColor(MdColor.BLUE_GREY)
                modifier.alignX(AlignmentX.Center)
                modifier.textAlign(AlignmentX.Center, AlignmentY.Center)
                modifier.font(font)
                modifier.size(250.dp, 80.dp)
                modifier.textColor(MdColor.LIGHT_GREEN)
                onUpdate {
                    //we update the value of the player's score so that
                    // it gets reflected inside the ui
                    player1Text= playerScore1
                    player2Text= playerScore2
                }
            }
        }
        //creating another panel for the instructions (note that I wasn't a great UI designer in libgdx
        // either so don't expect much 😅)
        Panel(colors = Colors.singleColorDark(MdColor.LIME)) {
            val font = MsdfFont(
                sizePts = fontSize/2.5f,
                glowColor = colors.primary.withAlpha(0.5f)
            )
            modifier
                .size(400.dp, 200.dp)
                .align(AlignmentX.End, AlignmentY.Bottom)
                .margin(top = 40.dp)
                .backgroundColor(Color.DARK_BLUE)

            Text("Use the up and down/left and right arrow keys \n" +
                    "to move the paddles"){
                modifier.alignX(AlignmentX.Center)
                modifier.textAlign(AlignmentX.Center, AlignmentY.Center)
                modifier.font(font)
                modifier.paddingTop=40.dp
                modifier.textColor(MdColor.LIGHT_GREEN)
            }
        }
    }
}

fun Scene.loadPhysicsWorld() {
    //we initialize the physic world, it will be responsible for simulating
    //and updating the physic actors in it
    world = PhysicsWorld().apply {
        registerHandlers(this@loadPhysicsWorld)
        simStepper = stepper
        //for this game we want the gravity to be set to 0
        gravity = Vec3f.ZERO
    }
}

fun Scene.loadPaddles() {
    //to create the paddles we need a physic actor (aka RigidDynamic) and
    //a mesh (model that will be rendered), we initialize the physic actor by giving
    //it a material (containing information about it's mass, friction, restitution ecc.)
    //and a shape
    val material = Material(0.5f, 0.5f, 1f)
    val paddleGeom = BoxGeometry(Vec3f(3f, 10f, 5f))
    val paddleBody = RigidDynamic(isKinematic = true).apply {
        attachShape(Shape(paddleGeom, material))
        position = Vec3f(-40f, 0f, 0f)
    }
    //we create the mesh for the paddle by using the colorMesh function
    val paddleMesh = colorMesh {
        generate {
            color = MdColor.GREEN
            //this will give the mesh the same shape of the physic actor
            paddleGeom.generateMesh(this)
        }
        shader = KslPbrShader {
            color { vertexColor() }
        }
        onUpdate {
            //on every update we want to make the mesh' and physic actor's
            //position and rotation match, to do this we set the mesh's transform
            //to be the same as the physic actor's one
            transform.set(paddleBody.transform)
        }
    }
    paddle1 = PongPaddle(paddleBody, paddleMesh, KeyboardInput.KEY_CURSOR_UP, KeyboardInput.KEY_CURSOR_DOWN)
    //same thing for the second paddle
    val paddle2Body= RigidDynamic(isKinematic = true).apply {
        attachShape(Shape(paddleGeom, material))
        position = Vec3f(40f, 0f, 0f)
    }
    val paddle2Mesh = colorMesh {
        generate {
            color = MdColor.GREEN
            paddleGeom.generateMesh(this)
        }
        shader = KslPbrShader {
            color { vertexColor() }
        }
        onUpdate {
            transform.set(paddle2Body.transform)
        }
    }
    paddle2= PongPaddle(paddle2Body, paddle2Mesh, KeyboardInput.KEY_CURSOR_LEFT, KeyboardInput.KEY_CURSOR_RIGHT)
    //we add the two paddle's physic actors to the world, so that they will
    //be updated every frame
    world.addActor(paddleBody)
    world.addActor(paddle2Body)
}

fun Scene.loadWalls() {
    //the material is the same for all the walls
    val wallMaterial = Material(0f, 0f, 1f)
    //creating the bottom wall
    val wallBottomGeom = BoxGeometry(Vec3f(wallDimension, 10f, 10f))
    val wallBottom = RigidStatic().apply {
        attachShape(Shape(wallBottomGeom, wallMaterial))
        position = Vec3f(0f, -50f, 0f)
        world.addActor(this)
        tags.put("object", "wall")
    }


    //creating the left wall
    val wallLeftGeom = BoxGeometry(Vec3f(10f, wallDimension, 10f))
    val wallLeft = RigidStatic().apply {
        attachShape(Shape(wallLeftGeom, wallMaterial))
        position = Vec3f(-50f, 0f, 0f)
        world.addActor(this)
        tags.put("object", "wall")
    }
    //creating the top wall
    val wallTopGeom = BoxGeometry(Vec3f(wallDimension, 10f, 10f))
    val wallTop = RigidStatic().apply {
        attachShape(Shape(wallTopGeom, wallMaterial))
        position = Vec3f(0f, 50f, 0f)
        world.addActor(this)
        tags.put("object", "wall")
    }
    //creating the right wall
    val wallRightGeom = BoxGeometry(Vec3f(10f, wallDimension, 10f))
    val wallRight = RigidStatic().apply {
        attachShape(Shape(wallRightGeom, wallMaterial))
        position = Vec3f(50f, 0f, 0f)
        world.addActor(this)
        tags.put("object", "wall")
    }

    //generating the meshes for the walls
    //bottom wall
    colorMesh {
        generate {
            color = MdColor.BLUE
            wallBottomGeom.generateMesh(this)
        }
        shader = KslPbrShader {
            color { vertexColor() }
        }
        onUpdate {
            //technically this is not necessary because the walls are not
            //going to move at all, I put it here just for demonstration
            transform.set(wallBottom.transform)
        }
    }
    //left wall
    colorMesh {
        generate {
            color = MdColor.BLUE

            wallLeftGeom.generateMesh(this)
        }
        shader = KslPbrShader {
            color { vertexColor() }
        }
        onUpdate {
            transform.set(wallLeft.transform)
        }
    }
    //top wall
    colorMesh {
        generate {
            color = MdColor.BLUE

            wallTopGeom.generateMesh(this)
        }
        shader = KslPbrShader {
            color { vertexColor() }
        }
        onUpdate {
            transform.set(wallTop.transform)
        }
    }
    //right wall
    colorMesh {
        generate {
            color = MdColor.BLUE

            wallRightGeom.generateMesh(this)
        }
        shader = KslPbrShader {
            color { vertexColor() }
        }
        onUpdate {
            transform.set(wallRight.transform)
        }
    }
}

fun Scene.loadBall() {
    //creating the ball physic actor
    val sphereMaterial = Material(0f, 0f, 1.5f)
    val sphereShape = SphereGeometry(1f)
    ball = RigidDynamic(pose = Mat4f().translate(0f, 0f, 0f)).apply {
        attachShape(Shape(sphereShape, sphereMaterial))

    }
    world.addActor(ball)
    //after it's been created, we give the ball a random velocity to start
    //the game
    var impulseY= Random.nextInt(-12..12)
    while(impulseY==0){
        impulseY= Random.nextInt(-12..12)
    }
    val impulse = Vec3f(12f, impulseY.toFloat(), 0f) //for testing
    ball.addImpulseAtPos(impulse, Vec3f.ZERO)

    //creating the mesh for the ball
    colorMesh {
        generate {
            color = MdColor.GREEN
            sphereShape.generateMesh(this)
        }
        shader = KslPbrShader {
            color { vertexColor() }
        }
        onUpdate {
            transform.set(ball.transform)
        }
    }
}

fun restartBall() {
    //start a timer of 1.5 seconds, when it reaches 0 the ball is given
    //a random speed to start another round
    respawnTimer -= Time.deltaT
    if (respawnTimer <= 0) {
        respawnTimer = 1.5f
        respawnBall = false
        var impulseY= Random.nextInt(-12..12)
        while(impulseY==0){
            impulseY= Random.nextInt(-12..12)
        }
        val impulse = Vec3f(12f, impulseY.toFloat(), 0f) //for testing
        ball.addImpulseAtPos(impulse, Vec3f.ZERO)
    }
}

fun checkBallPosition() {
    //if the ball goes behind eiter one of the players than
    //a point has been scored
    if (ball.position.x <= -43f) {
        playerScore2+=1
        resetBallPosition()
        respawnBall = true
    }

    if (ball.position.x >= 43f){
        playerScore1+=1
        resetBallPosition()
        respawnBall = true

    }

    if (respawnBall) {
        restartBall()
    }
}

fun resetBallPosition() {
    //reset the ball to be at the center of the game area
    ball.linearVelocity = Vec3f(0f, 0f, 0f)
    ball.position = Vec3f(0f, 0f, 0f)
    //also reset the paddles' position to their initial one
    paddle1.apply {
        body.position= Vec3f(-40f, 0f, 0f)
        speed=0f
    }
    paddle2.apply {
        body.position= Vec3f(40f, 0f, 0f)
        speed=0f
    }
}








