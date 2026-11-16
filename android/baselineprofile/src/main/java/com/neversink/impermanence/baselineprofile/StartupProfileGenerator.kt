package com.neversink.impermanence.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opens the app and walks its main screens without starting any timing (so no bells ring):
 * Days, a day's detail, the editor, Quick Sit and Settings.
 */
@RunWith(AndroidJUnit4::class)
class StartupProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = "com.neversink.impermanence") {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.text("Days")), 10_000)

        device.findObject(By.text("Opening Day"))?.let {
            it.click()
            device.wait(Until.hasObject(By.text("Edit")), 5_000)
            device.findObject(By.text("Edit"))?.click()
            device.waitForIdle()
            device.pressBack()
            device.waitForIdle()
            device.pressBack()
            device.wait(Until.hasObject(By.text("Days")), 5_000)
        }

        device.findObject(By.desc("Quick Sit"))?.let {
            it.click()
            device.wait(Until.hasObject(By.text("Start")), 5_000)
            device.pressBack()
            device.wait(Until.hasObject(By.text("Days")), 5_000)
        }

        device.findObject(By.desc("Settings"))?.let {
            it.click()
            device.waitForIdle()
            device.pressBack()
            device.wait(Until.hasObject(By.text("Days")), 5_000)
        }
    }
}
