/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-02-23 11:47
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

package com.github.rwsbillyang.spider.video

import com.github.rwsbillyang.spider.ISpider
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException

abstract class VideoSpider: ISpider {
    val log: Logger = LoggerFactory.getLogger("VideoSpider")
    /**
     * 过滤链接，获取http连接地址
     * @param originUrl 抖音app复制出的url "三里屯街拍，祝愿大家高考顺利 https://v.douyin.com/JNDRc6L/ 复制此链接，打开【抖音短视频】，直接观看视频！"
     * @return url地址 如： https://v.douyin.com/JNDRc6L/
     */
    open fun decodeHttpUrl(originUrl: String): String {
        val start = originUrl.indexOf("http")
        val end = originUrl.lastIndexOf("/")
        return originUrl.substring(start, end)
    }


    @Throws(IOException::class)
    fun getRedirectURL(conn: Connection): String? {
        return conn.followRedirects(false).timeout(10000).execute().header("location")
    }
    fun getRedirectURL(url: String): String? {
        return Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1")
            .ignoreContentType(true)
            .followRedirects(false).timeout(10000)
            .execute()
            .header("location")
    }
}