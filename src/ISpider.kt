/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-02-21 20:51
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

import org.jsoup.Connection
import org.jsoup.Jsoup

interface ISpider {

    /**
     * url是否合法正确
     */
    val regPattern: String

    /**
     * url格式不对时的提示信息
     */
    val errMsg: String

    /**
     * parse page
     */
    fun doParse(url: String): Map<String, String?>

    fun getConn(url: String, ua: Array<String> = Spider.uas[Spider.UAs_Mobile]): Connection {
        return Jsoup.connect(url)
            .userAgent(ua[ua.indices.random()])
            .ignoreContentType(true)
    }
    fun getRedirectURL(url: String, ua: Array<String> = Spider.uas[Spider.UAs_Mobile]): String? {
        return getConn(url, ua).followRedirects(false).timeout(10000).execute().header("location")
    }
}