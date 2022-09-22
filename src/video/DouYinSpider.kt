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
                getRedirectURL(url,Spider.uas[Spider.UAs_WX])
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
            val con = getConn(apiUrl, Spider.uas[Spider.UAs_WX])
            val res: Connection.Response = con.ignoreContentType(true).timeout(10000).execute()

            val resText = res.body()
            //log.info("resText=$resText")
            //https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json.md#json-elements
            val json = Json.parseToJsonElement(resText) as JsonObject

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

                                //403 Forbidden
                                //map[Spider.IMGURL] = video?.get("cover")?.jsonObject?.get("url_list")?.jsonArray?.firstOrNull()?.jsonPrimitive?.content?.split("?")?.first()
                                map[Spider.IMGURL] = e["music"]?.jsonObject?.get("cover_thumb")?.jsonObject?.get("url_list")?.jsonArray?.firstOrNull()?.jsonPrimitive?.content

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
            log.error("Exception: ${e.message},  url=$url")
        }

        map[Spider.RET] = Spider.KO
        map[Spider.MSG] = "获取内容时网络超时，请重试"

        return map
    }
}
//https://v.douyin.com/LcK3g27/   9.48 MJI:/ 调小音量‼️女生偷偷在用的小登西居然是……  https://v.douyin.com/LcK3g27/ 复制此链接，打开Dou音搜索，直接观看视频
//https://www.iesdouyin.com/share/video/6933174036600081671/
//三里屯街拍，祝愿大家高考顺利 https://v.douyin.com/JNDRc6L/ 复制此链接，打开【抖音短视频】，直接观看视频！
fun main() {
    DouYinSpider()
        .doParse("https://v.douyin.com/JNDRc6L/")
        .forEach {
            println("${it.key}=${it.value}")
        }
}
/*
val resText=
{
  "status_code": 0, "item_list": [{
    "comment_list": null, "promotions": null,
    "desc": "调小音量‼️女生偷偷在用的小登西居然是……"
    , "aweme_type": 4,
    "risk_infos": { "warn": false, "type": 0, "content": "", "reflow_unplayable": 0 },
    "music": {
      "cover_hd": {
        "uri": "1080x1080/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316",
        "url_list": ["https://p26.douyinpic.com/aweme/1080x1080/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316.jpeg?from=116350172",
          "https://p6.douyinpic.com/aweme/1080x1080/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316.jpeg?from=116350172",
          "https://p11.douyinpic.com/aweme/1080x1080/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316.jpeg?from=116350172"]
      },
      "cover_large": {
        "url_list": ["https://p26.douyinpic.com/aweme/1080x1080/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316.jpeg?from=116350172",
          "https://p6.douyinpic.com/aweme/1080x1080/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316.jpeg?from=116350172",
          "https://p11.douyinpic.com/aweme/1080x1080/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316.jpeg?from=116350172"],
        "uri": "1080x1080/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316"
      },
      "cover_thumb": {
        "uri": "168x168/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316",
        "url_list": ["https://p26.douyinpic.com/img/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316~c5_168x168.jpeg?from=116350172",
          "https://p6.douyinpic.com/img/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316~c5_168x168.jpeg?from=116350172",
          "https://p9.douyinpic.com/img/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316~c5_168x168.jpeg?from=116350172"]
      },
      "play_url": {
        "uri": "https://sf6-cdn-tos.douyinstatic.com/obj/ies-music/7051497898504129293.mp3",
        "url_list": ["https://sf6-cdn-tos.douyinstatic.com/obj/ies-music/7051497898504129293.mp3",
          "https://sf3-cdn-tos.douyinstatic.com/obj/ies-music/7051497898504129293.mp3"]
      },
      "duration": 81, "status": 1, "mid": "7051497902417447717",
      "title": "@果儿Dora创作的原声", "author": "果儿Dora",
      "cover_medium": {
        "uri": "720x720/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316",
        "url_list": ["https://p6.douyinpic.com/aweme/720x720/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316.jpeg?from=116350172",
          "https://p11.douyinpic.com/aweme/720x720/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316.jpeg?from=116350172",
          "https://p3.douyinpic.com/aweme/720x720/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316.jpeg?from=116350172"]
      },
      "position": null, "id": 7051497902417448000
    }, "is_live_replay": false, "share_info": {
      "share_title": "调小音量‼️女生偷偷在用的小登西居然是……",
      "share_weibo_desc": "#在抖音，记录美好生活#调小音量‼️女生偷偷在用的小登西居然是……",
      "share_desc": "在抖音，记录美好生活"
    }, "duration": 81583, "label_top_text": null,
    "video_labels": null, "video_text": null, "timer": { "status": 1, "public_time": 1641894000 },
    "group_id": 7051497717733821000,
    "author": {
      "policy_version": null, "uid": "100677191671", "nickname": "果儿Dora",
      "avatar_larger": {
        "uri": "1080x1080/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316",
        "url_list": ["https://p3.douyinpic.com/aweme/1080x1080/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316.jpeg?from=116350172",
          "https://p11.douyinpic.com/aweme/1080x1080/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316.jpeg?from=116350172",
          "https://p26.douyinpic.com/aweme/1080x1080/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316.jpeg?from=116350172"]
      },
      "avatar_thumb": {
        "uri": "100x100/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316",
        "url_list": ["https://p26.douyinpic.com/aweme/100x100/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316.jpeg?from=116350172",
          "https://p11.douyinpic.com/aweme/100x100/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316.jpeg?from=116350172",
          "https://p3.douyinpic.com/aweme/100x100/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316.jpeg?from=116350172"]
      },
      "unique_id": "", "followers_detail": null, "geofencing": null, "short_id": "1086635871",
      "signature": "美妆护肤卷王👑 \n半个富婆/黄一白/混油皮/单眼皮\n【掌握护肤密码🤫分享美女思路】\n🎯人生目标：把自己买的10000件小玩意给带🔥 \n❤️爱好：在视频下面啾🎁\n🤏🏻在线征集百人测评团👉抖音粉丝群\n日常发疯👉🏻@果鹅叨啦\n暴脾气，不接受无脑夸，合作V+：18018109455",
      "avatar_medium": {
        "uri": "720x720/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316",
        "url_list": ["https://p9.douyinpic.com/aweme/720x720/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316.jpeg?from=116350172",
          "https://p6.douyinpic.com/aweme/720x720/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316.jpeg?from=116350172",
          "https://p26.douyinpic.com/aweme/720x720/aweme-avatar/mosaic-legacy_2d20d0002ffba9508d316.jpeg?from=116350172"]
      },
      "platform_sync_info": null, "type_label": null
    },
    "share_url": "https://www.iesdouyin.com/share/video/7051497717733821709/?region=&mid=7051497902417447717&u_code=48&did=MS4wLjABAAAANwkJuWIRFOzg5uCpDRpMj4OX-QryoDgn-yYlXQnRwQQ&iid=MS4wLjABAAAANwkJuWIRFOzg5uCpDRpMj4OX-QryoDgn-yYlXQnRwQQ&with_sec_did=1&titleType=title",
    "text_extra": [], "forward_id": "0", "images": null, "group_id_str": "7051497717733821709",
    "video": {
      "bit_rate": null, "cover": {
        "url_list": ["https://p26-sign.douyinpic.com/tos-cn-i-dy/755cc5e987bb49bf838473e43a83402c~c5_300x400.jpeg?x-expires=1646481600&x-signature=j8%2B%2FIgT84BIo2QT32AWNxd5PxsE%3D&from=4257465056_large",
          "https://p9-sign.douyinpic.com/tos-cn-i-dy/755cc5e987bb49bf838473e43a83402c~c5_300x400.jpeg?x-expires=1646481600&x-signature=tGdXYrLV4vKZ6rEhGomEx1l4zhg%3D&from=4257465056_large", "https://p3-sign.douyinpic.com/tos-cn-i-dy/755cc5e987bb49bf838473e43a83402c~c5_300x400.jpeg?x-expires=1646481600&x-signature=H9SVtY%2FqDH%2For6aMZBhzgW29kA0%3D&from=4257465056_large"], "uri": "tos-cn-i-dy/755cc5e987bb49bf838473e43a83402c"
      }, "height": 1080, "dynamic_cover": { "uri": "tos-cn-i-dy/755cc5e987bb49bf838473e43a83402c", "url_list": ["https://p3-sign.douyinpic.com/obj/tos-cn-i-dy/755cc5e987bb49bf838473e43a83402c?x-expires=1646481600&x-signature=XlddaS4aog%2Ff86HCD3DXV1gBp7w%3D&from=4257465056_large", "https://p9-sign.douyinpic.com/obj/tos-cn-i-dy/755cc5e987bb49bf838473e43a83402c?x-expires=1646481600&x-signature=ChSuEGTqBkRHql9Aq2%2FsW5G6DY8%3D&from=4257465056_large", "https://p26-sign.douyinpic.com/obj/tos-cn-i-dy/755cc5e987bb49bf838473e43a83402c?x-expires=1646481600&x-signature=0fOKK7OjGjX5%2FZgChpqVEfLGTJI%3D&from=4257465056_large"] }, "origin_cover": { "uri": "tos-cn-p-0015/4eadfce2fad54f3a90b60d027efcd1ae_1641804801", "url_list": ["https://p9-sign.douyinpic.com/tos-cn-p-0015/4eadfce2fad54f3a90b60d027efcd1ae_1641804801~tplv-dy-360p.jpeg?x-expires=1646481600&x-signature=upQIW8QUUw5KiNOmRy2MNmoJmuw%3D&from=4257465056&s=&se=false&sh=&sc=&l=20220219201120010212162013395746B5&biz_tag=feed_cover", "https://p3-sign.douyinpic.com/tos-cn-p-0015/4eadfce2fad54f3a90b60d027efcd1ae_1641804801~tplv-dy-360p.jpeg?x-expires=1646481600&x-signature=EWbjgVYc2%2BFfYeQiFq1V5dQC68U%3D&from=4257465056&s=&se=false&sh=&sc=&l=20220219201120010212162013395746B5&biz_tag=feed_cover", "https://p6-sign.douyinpic.com/tos-cn-p-0015/4eadfce2fad54f3a90b60d027efcd1ae_1641804801~tplv-dy-360p.jpeg?x-expires=1646481600&x-signature=gAMgl4SwIwnKP6y%2FvfeXpV6CZ1g%3D&from=4257465056&s=&se=false&sh=&sc=&l=20220219201120010212162013395746B5&biz_tag=feed_cover"] }, "ratio": "540p", "has_watermark": true, "duration": 81583, "vid": "v0200fg10000c7dv7gbc77u9ght5r6l0", "play_addr": { "uri": "v0200fg10000c7dv7gbc77u9ght5r6l0", "url_list": ["https://aweme.snssdk.com/aweme/v1/playwm/?video_id=v0200fg10000c7dv7gbc77u9ght5r6l0&ratio=720p&line=0"] }, "width": 1920, "is_long_video": 1
    }, "author_user_id": 100677191671, "long_video": null, "create_time": 1641894000, "is_preview": 1, "statistics": { "aweme_id": "7051497717733821709", "comment_count": 1909, "digg_count": 52659, "play_count": 0, "share_count": 4611 }, "image_infos": null, "mix_info": { "mix_name": "富婆的10000件玩意~", "status": { "status": 2, "is_collected": 0 }, "statis": { "play_vv": 0, "collect_vv": 0, "current_episode": 29, "updated_to_episode": 37 }, "desc": "没有贵替！咱就是说嘎嘎好使~", "create_time": 1611712821, "next_info": { "mix_name": "富婆的10000件玩意~", "desc": "没有贵替！咱就是说嘎嘎好使~", "cover_url": { "uri": "tos-cn-i-0813/fae0c5b3a2f14a93aa9fe214a3988812", "url_list": ["https://p26-sign.douyinpic.com/obj/tos-cn-i-0813/fae0c5b3a2f14a93aa9fe214a3988812?x-expires=1645293600&x-signature=fjrnYwHdDzq0QlyVKHgSxyJHtZA%3D&from=747319617", "https://p3-sign.douyinpic.com/obj/tos-cn-i-0813/fae0c5b3a2f14a93aa9fe214a3988812?x-expires=1645293600&x-signature=6qd2uiMACHX5WI0J5PwRe6dJaUY%3D&from=747319617", "https://p6-sign.douyinpic.com/obj/tos-cn-i-0813/fae0c5b3a2f14a93aa9fe214a3988812?x-expires=1645293600&x-signature=aBSOGI2pt51XQOzdpkvoE7tTulA%3D&from=747319617"] } }, "mix_id": "6922253856113952776", "cover_url": { "url_list": ["https://p3-sign.douyinpic.com/obj/tos-cn-i-0813/fae0c5b3a2f14a93aa9fe214a3988812?x-expires=1645293600&x-signature=6qd2uiMACHX5WI0J5PwRe6dJaUY%3D&from=116350172", "https://p9-sign.douyinpic.com/obj/tos-cn-i-0813/fae0c5b3a2f14a93aa9fe214a3988812?x-expires=1645293600&x-signature=h3SYOBUTIK5Ag8YKqzLIpVdEDsE%3D&from=116350172", "https://p26-sign.douyinpic.com/obj/tos-cn-i-0813/fae0c5b3a2f14a93aa9fe214a3988812?x-expires=1645293600&x-signature=fjrnYwHdDzq0QlyVKHgSxyJHtZA%3D&from=116350172"], "uri": "tos-cn-i-0813/fae0c5b3a2f14a93aa9fe214a3988812" }, "extra": "{\"audit_locktime\":1639148454,\"ban_fields\":[],\"douyin_search_mix_copyright_block_area\":\"\",\"douyin_search_mix_lvideo_actors\":\" \",\"douyin_search_mix_lvideo_cid\":\"\",\"douyin_search_mix_lvideo_name\":\"\",\"douyin_search_mix_lvideo_tags\":\"\",\"douyin_search_mix_lvideo_type\":\"\",\"douyin_search_ocr_content\":\"没有 贵替 ！ 咱 就是说 嘎嘎 好使 ~\",\"douyin_search_user_generated_title\":\"      \",\"first_reviewed\":1,\"is_conflict\":false,\"is_quality_mix\":0,\"last_added_item_time\":1645242120,\"mix_earliest_video_creation_time\":1595064210,\"mix_latest_video_creation_time\":1644983994,\"mix_lvideo_quality_match\":0,\"mix_lvideo_quality_text\":0,\"mix_movie_commentary_type\":0,\"mix_ocr_mining_terms\":\"\",\"mix_title_mining_terms\":\"\",\"next_info\":{\"cover\":\"tos-cn-i-0813/fae0c5b3a2f14a93aa9fe214a3988812\",\"desc\":\"没有贵替！咱就是说嘎嘎好使~\",\"name\":\"富婆的10000件玩意~\"},\"segmentation\":\"富婆 的 10000 件 玩意 ~\"}" }, "category": 0, "aweme_id": "7051497717733821709", "cha_list": null, "geofencing": null
  }], "extra": { "now": 1645272680000, "logid": "20220219201120010212162013395746B5" }
}

* */