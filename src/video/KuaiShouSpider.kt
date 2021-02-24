/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-02-23 11:21
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

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.DomNode
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.github.rwsbillyang.spider.Spider

import java.io.IOException

fun DomNode.attr(name: String) = attributes.getNamedItem(name).textContent

/**
 * 动态网页，即使用htmlunit，也未成功，因htmlunit中js执行出错
 * TODO: 改为selenium或分析出
 * https://github.com/bajingxiaozi/video_parse/blob/master/src/main/java/com/xyf/video/parse/KuaishouLinkParse.java
 *
 * https://docs.tenapi.cn/kuaishou.html
 * */
class KuaiShouSpider: VideoSpider() {
    override val regPattern =  "[^x00-xff]*\\s*http(s)?://(\\w|-)+\\.kuaishou\\.com/\\S+\\s*[^x00-xff]*"
    override val errMsg = "请确认链接是否包含： https://v.kuaishou.com/"


    private val webClient = WebClient(BrowserVersion.BEST_SUPPORTED)
    init {
        with(webClient.options){
            isCssEnabled = false //（屏蔽)css 因为css并不影响我们抓取数据 反而影响网页渲染效率
            isThrowExceptionOnScriptError = false //（屏蔽)异常
            isThrowExceptionOnFailingStatusCode = false //（屏蔽)日志
            isDownloadImages = false
            isPopupBlockerEnabled = true
            isRedirectEnabled = true
            isActiveXNative = false
            isAppletEnabled = false
            isDoNotTrackEnabled = true
            isGeolocationEnabled = false
            isJavaScriptEnabled = true //加载js脚本
            timeout = 50000 //设置超时时间

        }
        webClient.waitForBackgroundJavaScript(10000)
        webClient.ajaxController = NicelyResynchronizingAjaxController() //设置ajax
    }

    override fun decodeHttpUrl(originUrl: String): String {
        val start = originUrl.indexOf("http")
        val end = originUrl.indexOf("复制")
        return originUrl.substring(start, end).trim()
    }

    // originUrl: #汪星人 #宠物避障挑战 https://v.kuaishou.com/5xXNiL 复制此链接，打开【快手App】直接观看！
    override fun doParse(url: String): Map<String, String?> {
        val map = mutableMapOf<String, String?>()
        try {
            val url2 = decodeHttpUrl(url)
            log.info("url=$url2")
            val con = getConn(url2)

            //https://c.kuaishou.com/fw/photo/3xmtv34g7te766m
            //https://video.kuaishou.com/short-video/3xmtv34g7te766m
            //https://c.kuaishou.com/fw/photo/3xmtv34g7te766m?fid=364977367&cc=share_copylink&shareMethod=TOKEN&docId=0&kpn=KUAISHOU&subBiz=PHOTO&photoId=3xmtv34g7te766m&shareId=174934779986&shareToken=X-9PkGU53YiW8AYq_A&shareResourceType=PHOTO_OTHER&userId=3x3ns26k8ma3mbw&shareType=1&et=1_i%2F0_unknown0&groupName=&appType=1&shareObjectId=28588605066&shareUrlOpened=0&timestamp=1589543938285

            val reUrl = getRedirectURL(con)
            log.info("reUrl=$reUrl")
            if(reUrl != null){
                val userId = reUrl.split("userId=")[1].split("&")[0] //得到userId：3x3ns26k8ma3mbw
                val vId = reUrl.split("?")[0].split("/").last() //得到videoId: 3xmtv34g7te766m

                //val url_1 = res2['Cookie'].split(";")[-1].replace(":","=")

                // val lastUrl = "https://live.kuaishou.com/u/"+userId+"/"+vId+"?"+url_1 //https://live.kuaishou.com/u/3x3ns26k8ma3mbw/3xmtv34g7te766m

                //https://htmlunit.sourceforge.io/gettingStarted.html
                val htmlPage: HtmlPage = webClient.getPage(reUrl) //将客户端获取的树形结构转化为HtmlPage

                log.info("doc="+htmlPage.body.asText())
                map[Spider.TITLE] = htmlPage.querySelector<DomNode>("meta[name=description]")?.attr("content")?.split("#")?.firstOrNull()
                map[Spider.IMGURL] = htmlPage.querySelector<DomNode>("link[rel=image-src]")?.attr("href")

                val element = htmlPage.querySelector<DomNode>("video#video-player")
                map[Spider.VIDEO_COVER] = element?.attr("poster")?.split("?")?.firstOrNull()

                val videoUrl = element?.attr("src")?.split("?")?.firstOrNull()
                if(videoUrl.isNullOrBlank()){
                    log.warn("fail to get videoUrl: originUrl=$url")
                    map[Spider.RET] = Spider.KO
                    map[Spider.MSG] = "fail to get videoUrl"
                    return map
                }else{
                    map[Spider.VIDEO] = videoUrl
                }

                map[Spider.LINK] = url
                map[Spider.USER] = htmlPage.querySelector<DomNode>("div.name")?.textContent

                map[Spider.RET] = Spider.OK
                map[Spider.MSG] = "恭喜，解析成功"

                return map
            }



        } catch (e: IOException) {
            e.printStackTrace()
        }

        map[Spider.RET] = Spider.KO
        map[Spider.MSG] = "获取内容时网络超时，请重试"
        return map
    }
}
