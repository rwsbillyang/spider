/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-02-27 16:22
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rwsbillyang.spider


import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeDriverService
import org.openqa.selenium.chrome.ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY
import org.openqa.selenium.chrome.ChromeOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException



/**
 * @deprecated("use seperated chromedriver process")
 * https://chromedriver.chromium.org/getting-started
 * */
object ChromeDriverServiceWrapper{
    private val log: Logger = LoggerFactory.getLogger("ChromeDriverServiceWrapper")
    val timeOut = 20L

    private var service: ChromeDriverService? = null
    private var driver: WebDriver? = null

    //@BeforeClass
    @Throws(IOException::class)
    fun createAndStartService(driverPath: String) {
        println("driverPath=$driverPath")
        System.setProperty(CHROME_DRIVER_EXE_PROPERTY, driverPath) // 必须加入
        service = ChromeDriverService.Builder()
            .usingDriverExecutable(File(driverPath))
            .usingAnyFreePort()
            .build()
        service?.start()
    }

    // eg: Spider.UAs_WX
    //@org.junit.Before
    fun createDriver(uas: Array<String>) {
        driver = ChromeDriver(chromeOptions(uas))
    }

    /**
     * 测试用例中，直接调用了createAndStartService和createDriver，无需用到此处参数
     * 生产环境中，只有第一个调用者使用该参数进行初始化，后面的调用者指定的参数已无效
     * */
    fun webDriver(driverPath: String, uas: Array<String>): WebDriver {
        if(service == null){
            createAndStartService(driverPath)
        }
        if(driver == null){
            createDriver(uas)
        }

        //TODO:修改ua，让随机化

        return driver!!
    }

    //@org.junit.After
    fun quitDriver() {
        if(driver != null){
            driver?.quit()
        }else{
            log.warn("webDriver is null when quitDriver")
        }
    }
    //@AfterClass
    fun stopService() {
        if(service != null){
            service?.stop()
        }else{
            log.warn("ChromeDriverService is null when stopService")
        }
    }

    private fun chromeOptions(uas: Array<String>) = ChromeOptions().apply {
        setHeadless(true) //已包含addArguments("--disable-gpu")
        addArguments("--user-agent="+uas[uas.indices.random()])
        addArguments("--blink-settings=imagesEnabled=false") //禁用图片加载
        addArguments("--incognito")
        addArguments("--window-size=480,720")
        addArguments("--disable-dev-shm-usage")
        addArguments("--disable-extensions")
        addArguments("--lang=zh-CN")
        addArguments("--disable-images")
        addArguments("--single-process")
        addArguments ("--no-sandbox")
        addArguments("--disable-blink-features=AutomationControlled")
        //设置开发者模式启动，该模式下webdriver属性为正常值
        setExperimentalOption("excludeSwitches", arrayOf("enable-automation"))

        //setBinary(binary?:"./chromedriver") //not work
    }
}