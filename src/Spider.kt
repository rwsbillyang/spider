/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-02-21 21:09
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

import com.github.rwsbillyang.spider.news.Spider163
import com.github.rwsbillyang.spider.news.ToutiaoSpider
import com.github.rwsbillyang.spider.news.WechatArticleSpider


object Spider {
    val UAs = arrayOf(
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 12_4_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 NetType/WIFI Language/zh_CN",
        "Mozilla/5.0 (Linux; Android 7.1.1; PRO 6s Build/NMF26O; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/66.0.3359.126 MQQBrowser/6.2 TBS/045008 Mobile Safari/537.36 MMWEBID/2921",
        "Mozilla/5.0 (Linux; Android 9; COR-AL10 Build/HUAWEICOR-AL10; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/66.0.3359.126 MQQBrowser/6.2 TBS/045008 Mobile Safari/537.36 MMWEBID/1039",
        "Mozilla/5.0 (Windows NT 5.1; rv:6.0.2) Gecko/20100101 Firefox/6.0.2"
    )


    const val RET = "ret"
    const val KO = "ko"
    const val MSG = "msg"
    const val OK = "ok"


    const val TITLE = "title"
    const val BRIEF = "brief"
    const val IMGURL = "imgUrl"
    const val CONTENT = "content"

    const val LINK = "link"

    const val USER = "user"
    const val USER2 = "user2"

    const val TAG = "tag"
    const val OGURL = "ogurl"

    const val VIDEO = "video"
    const val MUSIC = "music"
}


fun main(args: Array<String>) {
    //val spider = WechatArticleSpider()
    //val spider = Spider163()
    val spider = ToutiaoSpider()

    val map = mutableMapOf<String, String?>()
    //https://mp.weixin.qq.com/s/fBFQg0dDDBctl1fT3RlW0g  https://mp.weixin.qq.com/s/dgjpOWWz9P87I17OmFCGHQ

    spider.doParse("https://m.toutiao.com/i6525188057665110531/", map)
    map.forEach{
        println("${it.key}=${it.value}")
    }
}