package com.studybuddy.v2.ui.component

import com.studybuddy.v2.ui.pet.saddle.Pose
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 鞍部猫跨页持久状态。
 *
 * 真正的 application-scope MascotRoamer 改造工程量大且风险高（生命周期 / scope / Hilt 注入），
 * 这里用最小可行方案：保留 MascotRoamer 在 MascotDock 内 remember，但创建时从这里恢复
 * 上次的 theta / pose，stop 时回写。
 *
 * 效果：跨 tab 切换时鞍部猫位置不重置，姿态保留，**用户感觉是同一只猫一直在身边**。
 *
 * P6 接入跨页过渡动画时改为完整的 application-scope Roamer。
 */
@Singleton
class MascotPersistentState @Inject constructor() {
    @Volatile var lastTheta: Float = 0.5f
    @Volatile var lastPose: Pose = Pose.IDLE
    @Volatile var lastUpdatedMs: Long = 0L

    fun snapshot(theta: Float, pose: Pose) {
        lastTheta = theta
        lastPose = pose
        lastUpdatedMs = System.currentTimeMillis()
    }
}
