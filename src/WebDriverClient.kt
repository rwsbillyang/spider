/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-09-22 17:58
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

import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL
import org.openqa.selenium.TimeoutException
import java.io.IOException
import java.net.MalformedURLException

/**
 * Should first  $ ./chromedriver
 * */
abstract class WebDriverClient(val uaIndex: Int, private val imagesEnabled:Boolean = false) : ISpider {
    protected val log: Logger = LoggerFactory.getLogger("WebDriverClient")
    private var ua: String? = null
    abstract fun extract(url: String, map: MutableMap<String, String?>)
    override fun doParse(url: String): Map<String, String?> {
        log.info("doParse url=$url")

        val map = mutableMapOf<String, String?>()

        try {
            webDriver.get(url)

            extract(url, map)

        } catch (e: NoSuchElementException) {
            log.error("driver.currentUrl=${webDriver.currentUrl}, ua=$ua, NoSuchElementException: ${e.message}")
            map[Spider.MSG] = e.message
            map[Spider.RET] = Spider.KO
        } catch (e: TimeoutException) {
            log.error("driver.currentUrl=${webDriver.currentUrl}, ua=$ua, TimeoutException: ${e.message}")
            map[Spider.MSG] = e.message
            map[Spider.RET] = Spider.KO
        } catch (e: MalformedURLException) {
            log.error("url=$url, ua=$ua, MalformedURLException: ${e.message}")
            map[Spider.MSG] = e.message
            map[Spider.RET] = Spider.KO
        } catch (e: IOException) {
            log.error("url=$url, ua=$ua, IOException: ${e.message}")
            map[Spider.MSG] = e.message
            map[Spider.RET] = Spider.KO
        } catch (e: Exception) {
            log.error("url=$url, ua=$ua, Exception: ${e.message}")
            map[Spider.MSG] = e.message
            map[Spider.RET] = Spider.KO
        } finally {
            webDriver.quit()
        }
        return map
    }

    private val chromeOptions = ChromeOptions().apply {
        setHeadless(true) //已包含addArguments("--disable-gpu")
        ua = Spider.uas[uaIndex][Spider.uas[uaIndex].indices.random()]
        addArguments("--user-agent=$ua")
        addArguments("--blink-settings=imagesEnabled=$imagesEnabled") //禁用图片加载
        if(!imagesEnabled)addArguments("--disable-images")
        addArguments("--incognito")
        if(uaIndex == Spider.UAs_PC)addArguments("--window-size=1280,800") //wxh
        else addArguments("--window-size=720,1280")
        addArguments("--disable-dev-shm-usage")
        addArguments("--disable-extensions")
        addArguments("--lang=zh-CN")
        addArguments("--single-process")
        addArguments("--no-sandbox")
        addArguments("--disable-blink-features=AutomationControlled")
        //设置开发者模式启动，该模式下webdriver属性为正常值
        setExperimentalOption("excludeSwitches", arrayOf("enable-automation"))

        //setCapability("platformName", "Mac OS");

        //setBinary(binary?:"./chromedriver") //not work
    }

    val webDriver = RemoteWebDriver(URL("http://127.0.0.1:9515"), chromeOptions, false)
}