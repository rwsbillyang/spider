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
import java.net.URL

/**
 * Should first  $ ./chromedriver
 * */
abstract class WebDriverClient(val uaIndex: Int) : ISpider{

    private val chromeOptions = ChromeOptions().apply {
        setHeadless(true) //已包含addArguments("--disable-gpu")
        addArguments("--user-agent="+Spider.uas[uaIndex][Spider.uas[uaIndex].indices.random()])
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

    val webDriver = RemoteWebDriver(URL("http://127.0.0.1:9515"), chromeOptions)
}