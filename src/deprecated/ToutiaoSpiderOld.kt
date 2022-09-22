/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-09-16 21:43
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

package com.github.rwsbillyang.spider.deprecated

import com.github.rwsbillyang.spider.*
import com.github.rwsbillyang.spider.utils.HtmlImgUtil
import kotlinx.serialization.json.*
import org.jsoup.Connection

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException


/**
 * @deprecated("头条升级导致不能使用")
 * */
class ToutiaoSpiderOld : ISpider {
    private val log: Logger = LoggerFactory.getLogger("ToutiaoSpider")
    override val regPattern = "http(s)?://(m|www)\\.toutiao(cdn)?\\.(com|cn|net)/(a|i|article)\\S+"
    override val errMsg =
        "只支持头条文章和微头条 链接中有这些字符：toutiao.com 或 toutiaocdn.com"

    //https://www.toutiao.com/i6525188057665110531/ -> https://m.toutiao.com/i6525188057665110531/info/v2/
    //https://m.toutiao.com/i6525188057665110531/ -> https://m.toutiao.com/i6525188057665110531/info/v2/
    //https://www.toutiao.com/a6931886311808827912/ -> https://m.toutiao.com/i6931886311808827912/info/v2/
    //https://m.toutiao.com/i6932388257619657228/ -> https://m.toutiao.com/i6932388257619657228/info/v2/
    override fun doParse(url: String): Map<String, String?> {
        val map = mutableMapOf<String, String?>()
        val url2 = intercept(url)
        if(url2 == null){
            map[Spider.RET] = Spider.KO
            map[Spider.MSG] = "出错了，换个链接试试吧"
            return map
        }
        if(url2.contains(".zjurl.cn/") || url2.contains(".wukong.com/")){
            map[Spider.RET] = Spider.KO
            map[Spider.MSG] = "暂不支持今日头条中的悟空问答，换篇文章试试吧"
            return map
        }

        log.info("parse url=$url2")
        val newUrl = url2.replace(Regex("toutiao(cdn|img).(com|cn|net)"), "toutiao.com").split("?").first()
        log.info("newUrl=$newUrl")
        val id = newUrl.substringAfter("toutiao.com/").substringBefore('/').replace('a', 'i')
        val apiUrl = "https://m.toutiao.com/$id/info/v2/"
        log.info("apiUrl=$apiUrl")


        try {
            val con = getConn(apiUrl)
            val res: Connection.Response = con.ignoreContentType(true).timeout(10000).execute()
            val json = Json.parseToJsonElement(res.body()) as? JsonObject
            val isSuccess = json?.get("success")?.jsonPrimitive?.boolean ?: false
            if (isSuccess) {
                json?.get("data")?.jsonObject?.let {
                    map[Spider.RET] = Spider.OK
                    map[Spider.LINK] = newUrl

                    val content = it["content"]?.jsonPrimitive?.content
                    if (content.isNullOrBlank()) { //微头条
                        val thread = it["thread"]?.jsonObject

                        val threadBase = thread?.get("thread_base")?.jsonObject
                        map[Spider.CONTENT] = threadBase?.get("content")?.jsonPrimitive?.content?.replace("\n","<br>")
                        map[Spider.USER] = threadBase?.get("user")?.jsonObject?.get("info")?.jsonObject?.get("name")?.jsonPrimitive?.content
                        map[Spider.TITLE] = threadBase?.get("title")?.jsonPrimitive?.content?.substring(0, 30)

                        val share = thread?.get("share")?.jsonObject
                        map[Spider.IMGURL] = share?.get("share_icon")?.jsonPrimitive?.content
                        map[Spider.BRIEF] = share?.get("share_desc")?.jsonPrimitive?.content
                    } else {
                        //来自今日头条的视频
                        if(content.contains("tt-videoid=")){
                            //return ToutiaoVideoSpider().doParse(newUrl)
                            log.info("please use ToutiaoVideoSpider")
                        }else{
                            //正常的头条内容
                            map[Spider.CONTENT] = content
                            map[Spider.USER] = it["source"]?.jsonPrimitive?.content
                            map[Spider.TITLE] = it["title"]?.jsonPrimitive?.content
                            map[Spider.IMGURL] = HtmlImgUtil.getImageSrc(content)?.firstOrNull()
                                ?: it["media_user"]?.jsonObject?.get("avatar_url")?.jsonPrimitive?.content
                            //map[Spider.BRIEF] = it.get("")
                        }
                    }

                    return map
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        map[Spider.RET] = Spider.KO
        map[Spider.MSG] = "此链接(如第三方内容)暂不支持，换一篇文章试试吧"

        return map
    }

    //今日头条中的链接：https://m.toutiao.com/is/eLSqD9x/
    private fun intercept(url: String): String?{
        if(!url.contains("/is/")) return url

        return getRedirectURL(url)
    }

    private fun doParseBasedInJsoup(url: String, map: MutableMap<String, String?>) {
        try {
            val doc: Document =
                Jsoup.connect(url).timeout(20 * 1000).userAgent(Spider.UAs_WX[Spider.UAs_WX.indices.random()])
                    .followRedirects(true).get()

            //https://m.toutiao.com/i6931886311808827912/info/v2/
            var text: String = doc.select("title").text()
            map[Spider.TITLE] = text

            text = doc.select("meta[name=description]").attr("content")
            map[Spider.BRIEF] = text

//            text = doc.select("meta[property=og:image]").attr("content")
//            map[Spider.IMGURL] = text

            val es: Elements = doc.select("article")
            if (es.size > 0) {
                text = es[0].html()
                map[Spider.CONTENT] = text

                //图片地址为空，从正文内容中寻找第一张
                val list = HtmlImgUtil.getImageSrc2(text)
                if (!list.isNullOrEmpty()) {
                    map[Spider.IMGURL] = list[0]
                }
            }


            map[Spider.LINK] = url
            map[Spider.USER] = "今日头条"

            map[Spider.RET] = Spider.OK
            map[Spider.MSG] = "恭喜，解析成功，请编辑保存！"
        } catch (e: IOException) {
            e.printStackTrace()
            map[Spider.RET] = Spider.KO
            map[Spider.MSG] = "获取文章内容超时，请重试"
        }
    }
}

fun main(args: Array<String>) {
    ToutiaoSpiderOld().doParse("https://www.toutiao.com/article/7132394799076311584/?app=news_article&timestamp=1663290936&use_new_style=1&req_id=202209160915350101960350661A25B143&group_id=7132394799076311584&share_token=63da7242-3e4d-48d6-8676-42d6ca662a06&tt_from=copy_link&utm_source=copy_link&utm_medium=toutiao_android&utm_campaign=client_share&source=m_redirect&wid=1663291414789")
        .forEach {
            println("${it.key}=${it.value}")
        }
}