package pongTutorial

import de.fabmax.kool.input.KeyboardInput
import de.fabmax.kool.math.MutableVec3f
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.physics.RigidDynamic
import de.fabmax.kool.scene.Mesh

class PongPaddle2(var body: RigidDynamic, var mesh: Mesh) {

    var speed=0f

    init {
        KeyboardInput.addKeyListener(KeyboardInput.KEY_CURSOR_LEFT, "move up", filter = {it.isPressed}){
            speed= 0.5f

        }
        KeyboardInput.addKeyListener(KeyboardInput.KEY_CURSOR_RIGHT, "move down", filter = {it.isPressed}){
            speed= -0.5f
        }
    }

    fun update(){

        body.setKinematicTarget(body.position.add(Vec3f(0f, speed, 0f), MutableVec3f()))
    }
}