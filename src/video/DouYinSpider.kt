/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-02-22 21:09
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



import com.github.rwsbillyang.spider.Spider
import kotlinx.serialization.json.*
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException



class DouYinSpider {
    val log: Logger = LoggerFactory.getLogger("DouYinSpider")
    /**
     * 过滤链接，获取http连接地址
     * @param url 抖音app复制出的url "三里屯街拍，祝愿大家高考顺利 https://v.douyin.com/JNDRc6L/ 复制此链接，打开【抖音短视频】，直接观看视频！"
     * @return url地址 如： https://v.douyin.com/JNDRc6L/
     */
    private fun decodeHttpUrl(url: String): String {
        val start = url.indexOf("http")
        val end = url.lastIndexOf("/")
        return url.substring(start, end)
    }

    private fun getConn(url: String): Connection {
        return Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1")
            .ignoreContentType(true)
    }
    @Throws(IOException::class)
    private fun getRedirectURL(conn: Connection): String {
        return conn.followRedirects(false).timeout(10000).execute().header("location")
    }

    // originUrl:  "三里屯街拍，祝愿大家高考顺利 https://v.douyin.com/JNDRc6L/ 复制此链接，打开【抖音短视频】，直接观看视频！"
    fun doParse(originUrl: String, map: MutableMap<String, String?>){
        try {
            val url = decodeHttpUrl(originUrl)
            var con = getConn(url)
            //https://www.iesdouyin.com/share/video/6846660517122084096/?region=CN&mid=6846660529788947213&u_code=kja81591&titleType=title&utm_source=copy_link&utm_campaign=client_share&utm_medium=android&app=aweme
            val reUrl = getRedirectURL(con)
            val rest = reUrl.split("video/").toTypedArray()
            val mid = rest[1].split("/").toTypedArray()
            val apiUrl = "https://www.iesdouyin.com/web/api/v2/aweme/iteminfo/?item_ids=" + mid[0]

            //https://www.iesdouyin.com/web/api/v2/aweme/iteminfo/?item_ids=6846660517122084096
            con = getConn(apiUrl)

            val res: Connection.Response = con.ignoreContentType(true).timeout(10000).execute()

            //https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json.md#json-elements
            val json = Json.parseToJsonElement(res.body()) as JsonObject

            when(val list = json["item_list"]) {
                is JsonArray -> {
                    if(list.isEmpty()){
                        log.warn("no element in item_list")
                    }else{
                        when(val e = list[0]){
                            is JsonObject -> {
                                map[Spider.RET] = Spider.OK
                                map[Spider.LINK] = url

                                map[Spider.USER] = e["author"]?.jsonObject?.get("nickname")?.jsonPrimitive?.content
                                map[Spider.TITLE] = e["desc"]?.jsonPrimitive?.content

                                val video = e["video"]?.jsonObject
                                map[Spider.IMGURL] = video?.get("cover")?.jsonObject?.get("url_list")?.jsonArray?.firstOrNull()?.jsonPrimitive?.content
                                map[Spider.VIDEO] = video?.get("play_addr")?.jsonObject?.get("url_list")?.jsonArray?.firstOrNull()?.jsonPrimitive?.content?.replace("playwm", "play")

                                map[Spider.MUSIC] = e["music"]?.jsonObject?.get("play_url")?.jsonObject?.get("uri")?.jsonPrimitive?.content

                                return
                            }
                            else -> {
                                log.warn("element in item_list is not a JsonObject")
                            }
                        }
                    }
                }
                else -> {
                    log.warn("is not list of item_list")
                }
            }

        } catch (e: Exception) {
            log.error("caused Exception, the url=$originUrl")
        }

        map[Spider.RET] = Spider.KO
        map[Spider.MSG] = "获取内容时网络超时，请重试"
    }


}

fun main(args: Array<String>) {
    val spider = DouYinSpider()
    val map = mutableMapOf<String, String?>()

    spider.doParse("三里屯街拍，祝愿大家高考顺利 https://v.douyin.com/JNDRc6L/ 复制此链接，打开【抖音短视频】，直接观看视频！", map)
    map.forEach{
        println("${it.key}=${it.value}")
    }
}