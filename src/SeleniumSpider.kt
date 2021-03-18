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

import org.openqa.selenium.chrome.ChromeOptions

abstract class SeleniumSpider(binary: String? = null, uas: Array<String> = Spider.UAs_WX): ISpider {
    override val regPattern = "[^x00-xff]*\\s*http(s)?://(\\w|-)+\\.kuaishou(app)?\\.com/\\S+\\s*[^x00-xff]*"
    override val errMsg = "请确认链接是否包含： https://v.kuaishou.com/ 或 https://v.kuaishouapp.com/"

    init {
        System.setProperty("webdriver.chrome.driver", binary?:"./chromedriver") // 必须加入
    }
    val chromeOptions = ChromeOptions().apply {
        setHeadless(true) //已包含addArguments("--disable-gpu")
        addArguments("--user-agent="+uas[uas.indices.random()])
        addArguments("--blink-settings=imagesEnabled=false") //禁用图片加载
        addArguments("--incognito")
        addArguments("--window-size=360,640")
        addArguments("--disable-dev-shm-usage")
        addArguments("--disable-extensions")
        addArguments("--lang=zh-CN")
        addArguments("--disable-images")
        addArguments("--single-process")
        addArguments ("--no-sandbox")

        //setBinary(binary?:"./chromedriver") //not work
    }

}