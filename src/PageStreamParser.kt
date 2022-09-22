/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-02-21 20:56
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

import com.github.rwsbillyang.spider.utils.ExtractRule
import com.github.rwsbillyang.spider.utils.HtmlParser
import com.github.rwsbillyang.spider.utils.LoopMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*
import java.net.URL
import java.net.URLConnection

/**
 * 对请求获取的网页字符串流进行处理，逐行检查获取所需的值
 * */
abstract class PageStreamParser(private val uas: Array<String>, private val loopMode: LoopMode = LoopMode.ByLine) {
    val log: Logger = LoggerFactory.getLogger("PageStreamParser")

    abstract val extractRules: Array<ExtractRule>

    fun getPageAndParse(url: String, map: MutableMap<String, String?>, encode: String = "UTF-8") {
        val html = getPageHtml(url, encode)
        if(html != null){
            val htmlParser = HtmlParser(loopMode)
            htmlParser.parse(html, extractRules, map)
        }
    }

    //https://www.cnblogs.com/milton/p/6366916.html
    private fun inputStream2String(inputStream: InputStream): String{
        val result = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } != -1) {
            result.write(buffer, 0, length)
        }
        return result.toString("UTF-8")
    }


    private fun getPageHtml(url: String, encode: String): String? {
        log.info("parse url=$url")
        try {
            //建立请求链接
            val conn: URLConnection = URL(url).openConnection()
            conn.doInput = true
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.defaultUseCaches = true
            conn.setRequestProperty("User-agent", uas[uas.indices.random()])
            conn.setRequestProperty("Charset",encode)
            conn.connect() //本方法不会自动重连

            val inputStream = conn.getInputStream()
            if(inputStream != null){
                val html = inputStream2String(inputStream)
                inputStream.close()//关闭InputStream
                return html
            }
        } catch (e: IOException) {
            log.warn("IOException, url=$url")
        } catch (e: Exception) {
            log.warn("Exception, url=$url")
            e.printStackTrace()
        }
        return null
    }


}