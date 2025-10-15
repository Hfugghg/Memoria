package com.exp.memoria.core.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.exp.memoria.domain.usecase.ProcessMemoryUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * [后台记忆处理工作器]
 *
 * 职责:
 * 1. 使用WorkManager实现一个在后台可靠执行的任务 [cite: 82]。
 * 2. 此任务负责将积压的、状态为"NEW"的记忆进行处理，使其变为可检索的“冷记忆”。
 * 3. 工作流程：
 * a. 从 MemoryRepository 获取待处理的记忆列表。
 * b. 遍历列表，对每一条记忆调用 ProcessMemoryUseCase。
 * 4. 配置WorkManager的约束(Constraints)，例如只在设备充电或空闲时执行，以优化资源消耗 [cite: 46]。
 *
 * 关联:
 * - 这是一个继承自 CoroutineWorker 的类。
 * - 需要通过Hilt的辅助注入机制来获取其依赖，如 ProcessMemoryUseCase。
 * - ChatViewModel 会在每次对话结束后，向WorkManager调度一个新的、一次性的工作请求来运行这个Worker。
 *
 * 实现指导:
 * - 继承自 `CoroutineWorker` 并使用 `@HiltWorker` 注解。
 * - 在构造函数中注入 `ProcessMemoryUseCase` 和 `MemoryRepository`。
 * - 在 `doWork()` 方法中:
 * a. 调用 `MemoryRepository.getMemoriesToProcess()` 获取所有状态为"NEW"的记忆列表 [cite: 67]。
 * b. 遍历列表，对每一条记忆调用 `ProcessMemoryUseCase(...)`。
 * c. 根据处理结果返回 `Result.success()` 或 `Result.failure()`。
 * - 在ViewModel中构建WorkRequest时，可以设置Constraints，如 `setRequiredNetworkType`, `setRequiresCharging(true)` 等 [cite: 46]。
 */

@HiltWorker
class MemoryProcessingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val processMemoryUseCase: ProcessMemoryUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // 从输入数据中获取Long类型的memoryId，如果不存在则任务失败
        val memoryId = inputData.getLong(KEY_MEMORY_ID, -1)
        if (memoryId == -1L) {
            return Result.failure()
        }

        return try {
            // 调用用例处理该记忆
            processMemoryUseCase(memoryId)
            // 任务成功
            Result.success()
        } catch (e: Exception) {
            // 如果发生异常，任务失败
            Result.failure()
        }
    }

    companion object {
        const val KEY_MEMORY_ID = "KEY_MEMORY_ID"
    }
}
