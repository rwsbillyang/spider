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



import com.github.rwsbillyang.spider.ISpider
import com.github.rwsbillyang.spider.Spider
import kotlinx.serialization.json.*
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * 抖音的视频地址 经常变换，需要经常解析
 * 解析后的地址，会被微信拦截，少数机型没有拦截
 * */
class DouYinSpider: ISpider{
    private val log: Logger = LoggerFactory.getLogger("DouYinSpider")
    override val regPattern = "[^x00-xff]*\\s*http(s)?://(\\w|-)+\\.(ies)?douyin\\.com/\\S+\\s*[^x00-xff]*"
    override val errMsg = "请确认链接是否包含： https://v.douyin.com/ 等字符"

    /**
     * 过滤链接，获取http连接地址
     * @param originUrl 抖音app复制出的url "三里屯街拍，祝愿大家高考顺利 https://v.douyin.com/JNDRc6L/ 复制此链接，打开【抖音短视频】，直接观看视频！"
     * @return url地址 如： https://v.douyin.com/JNDRc6L/
     */
    private fun decodeHttpUrl(originUrl: String): String {
        val start = originUrl.indexOf("http")
        val end = originUrl.lastIndexOf("/")
        return originUrl.substring(start, end)
    }


    @Throws(IOException::class)
    fun getRedirectURL(conn: Connection): String? {
        return conn.followRedirects(false).timeout(10000).execute().header("location")
    }
//    fun getRedirectURL(url: String): String? {
//        return Jsoup.connect(url)
//            .userAgent(Spider.UAs_WX[Spider.UAs_WX.indices.random()])
//            .ignoreContentType(true)
//            .followRedirects(false).timeout(10000)
//            .execute()
//            .header("location")
//    }

    // url:  "三里屯街拍，祝愿大家高考顺利 https://v.douyin.com/JNDRc6L/ 复制此链接，打开【抖音短视频】，直接观看视频！"
    override fun doParse(url: String): Map<String, String?>{
        log.info("doParse, url=$url")
        val map = mutableMapOf<String, String?>()
        try {
            //val extractedUrl = decodeHttpUrl(url)
            //log.info("after decode, extractedUrl=$extractedUrl")
            val iesUrl = if(url.contains("iesdouyin.com/share/video/"))
            {
                url
            }else{
                //https://www.iesdouyin.com/share/video/6846660517122084096/?region=CN&mid=6846660529788947213&u_code=kja81591&titleType=title&utm_source=copy_link&utm_campaign=client_share&utm_medium=android&app=aweme
                getRedirectURL(url,Spider.UAs_WX)
            }
            log.info("parse iesUrl=$iesUrl")

            val mid = iesUrl?.substringAfter("/video/")?.split("/")?.firstOrNull()
            if(mid.isNullOrBlank()){
                log.warn("invalid url, no mid")
                map[Spider.RET] = Spider.KO
                map[Spider.MSG] = "获取内容失败"
                return map
            }

            val apiUrl = "https://www.iesdouyin.com/web/api/v2/aweme/iteminfo/?item_ids=$mid"

            //https://www.iesdouyin.com/web/api/v2/aweme/iteminfo/?item_ids=6846660517122084096
            val con = getConn(apiUrl, Spider.UAs_WX)
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
                                map[Spider.IMGURL] = video?.get("cover")?.jsonObject?.get("url_list")?.jsonArray?.firstOrNull()?.jsonPrimitive?.content?.split("?")?.first()

                                //https://aweme.snssdk.com/aweme/v1/playwm/?video_id=v0200f710000bs23isv327mm1tie6pe0&ratio=720p&line=0
                                val url1 = video?.get("play_addr")?.jsonObject?.get("url_list")?.jsonArray?.firstOrNull()?.jsonPrimitive?.content?.replace("playwm", "play")?.split("&")?.first()
                                //videoUrl = url1?.let { getRedirectURL(it)?.split("?")?.firstOrNull() }?:url1
                                if (url1.isNullOrBlank()) {
                                    log.warn("fail to get videoUrl: originUrl=$url")
                                    map[Spider.RET] = Spider.KO
                                    map[Spider.MSG] = "fail to get videoUrl"
                                    return map
                                } else {
                                    map[Spider.VIDEO] = url1
                                }

                                //map[Spider.MUSIC] = e["music"]?.jsonObject?.get("play_url")?.jsonObject?.get("uri")?.jsonPrimitive?.content

                                return map
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
            log.error("caused Exception, the url=$url")
        }

        map[Spider.RET] = Spider.KO
        map[Spider.MSG] = "获取内容时网络超时，请重试"

        return map
    }
}

//https://www.iesdouyin.com/share/video/6933174036600081671/
//三里屯街拍，祝愿大家高考顺利 https://v.douyin.com/JNDRc6L/ 复制此链接，打开【抖音短视频】，直接观看视频！
fun main(args: Array<String>) {
    DouYinSpider()
        .doParse("https://v.douyin.com/JNDRc6L/")
        .forEach {
            println("${it.key}=${it.value}")
        }
}
