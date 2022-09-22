/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-09-16 16:06
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

package com.github.rwsbillyang.spider.test

import com.github.rwsbillyang.spider.ChromeDriverServiceWrapper
import com.github.rwsbillyang.spider.Spider
import org.junit.AfterClass
import org.junit.BeforeClass
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// skip test: ./gradlew build -x test
open class SpiderTestBase {
    companion object {
        const val driverPath = "/Users/bill/git/youke/server/app/zhiKe/chromedriver"
        val uas = Spider.UAs_WX

        @JvmStatic
        @BeforeClass
        @Throws(IOException::class)
        fun createAndStartService() {
            ChromeDriverServiceWrapper.createAndStartService(driverPath)
        }

        @JvmStatic
        @AfterClass
        fun stopService() {
            ChromeDriverServiceWrapper.stopService()
        }
    }

    @org.junit.Before
    open fun createDriver() {
        ChromeDriverServiceWrapper.createDriver(uas)
    }


    @org.junit.After
    open fun quitDriver() {
        ChromeDriverServiceWrapper.quitDriver()
    }


    open fun newsTest(url: String) {
        assertTrue { Spider.verifyUrl(url) }

        val map = Spider.parse(url)

        assertEquals(Spider.OK, map[Spider.RET])
        assertFalse("title is empty or null") { map[Spider.TITLE].isNullOrEmpty() }
        assertFalse("content is empty or null") { map[Spider.CONTENT].isNullOrEmpty() }
    }

    open fun videoTest(url: String) {
        assertTrue { Spider.verifyUrl(url) }

        val map = Spider.parse(url)

        assertEquals(Spider.OK, map[Spider.RET])
        //assertFalse("title is empty or null") { map[Spider.TITLE].isNullOrEmpty() }
        assertFalse("content is empty or null") { map[Spider.VIDEO].isNullOrEmpty() }
    }
}


fun main() {
    Spider.parse("https://m.toutiaocdn.net/a6940884912362865183/?app=news_article&is_hit_share_recommend=0&share_token=a146bed7-329d-409d-8f54-77be583cd85b&tt_from=copy_link&utm_source=copy_link&utm_medium=toutiao_android&utm_campaign=client_share")
        .forEach {
            println("${it.key}=${it.value}")
        }

//    println(Spider.verifyUrl("http://v3-default.ixigua.com/401a132eecec653be1cb0061eef14af6/603a26d8/video/tos/cn/tos-cn-ve-4/38caad6af0f649b680c5d6ea37146e06/"))
//    println(Spider.verifyUrl("http://v6-default.ixigua.com/f336900ffba785ef2092136c6e911741/603a51a5/video/tos/cn/tos-cn-ve-4/38caad6af0f649b680c5d6ea37146e06/"))

    //https://mp.weixin.qq.com/s/fBFQg0dDDBctl1fT3RlW0g  https://mp.weixin.qq.com/s/dgjpOWWz9P87I17OmFCGHQ
//    Spider.parse("https://mp.weixin.qq.com/s/9ESoDV7-Fo6_mHvYC69W9w").forEach {
//        println("${it.key}=${it.value}")
//    }

//    Spider.parse("https://3g.163.com/news/article/G3K55190000189FH.html?clickfrom=index2018_news_newslist#offset=0")
//        .forEach {
//            println("${it.key}=${it.value}")
//        }
//
//    Spider.parse("https://m.toutiao.com/i6525188057665110531/")
//        .forEach {
//            println("${it.key}=${it.value}")
//        }
//
//    Spider.parse(" #汪星人 #宠物避障挑战 https://v.kuaishou.com/5xXNiL 复制此链接，打开【快手App】直接观看！")
//        .forEach {
//            println("${it.key}=${it.value}")
//        }
//
//    Spider.parse("三里屯街拍，祝愿大家高考顺利 https://v.douyin.com/JNDRc6L/ 复制此链接，打开【抖音短视频】，直接观看视频！")
//        .forEach {
//            println("${it.key}=${it.value}")
//        }
}
