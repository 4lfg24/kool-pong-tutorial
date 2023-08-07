package pongTutorial

import de.fabmax.kool.input.KeyboardInput
import de.fabmax.kool.input.UniversalKeyCode
import de.fabmax.kool.math.MutableVec3f
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.physics.RigidDynamic
import de.fabmax.kool.scene.Mesh
import kotlin.math.abs

class PongPaddle(var body:RigidDynamic, var mesh: Mesh, moveUpButton:UniversalKeyCode, moveDownButton:UniversalKeyCode) {

    var speed=0f

    init {
        KeyboardInput.addKeyListener(moveUpButton, "move up", filter = {it.isPressed}){
            //whenever the up key is pressed we want to move the paddle up
            speed= 0.5f
        }
        KeyboardInput.addKeyListener(moveDownButton, "move down", filter = {it.isPressed}){
            //same thing for when the down key is pressed
            speed= -0.5f
        }
        //for the contact listener
        body.tags.put("object", this)
    }

    fun update(){
        //this is necessary when using kinematic actors, because they act
        //different compared to normal rigid actors to update their position
        //you need to first update their target's position, for further explanation
        //check the PhysX documentation at: https://nvidia-omniverse.github.io/PhysX/physx/5.1.3/docs/RigidBodyDynamics.html#kinematic-actors
        body.setKinematicTarget(body.position.add(Vec3f(0f, speed, 0f), MutableVec3f()))
        //if the paddle reaches the top or bottom wall invert its speed
        if (abs(body.position.y)>=40f){
            body.setKinematicTarget(body.position.subtract(Vec3f(0f, speed, 0f), MutableVec3f()))
            speed*=-1
        }
    }

}