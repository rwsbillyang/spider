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

    fun getConn(url: String): Connection {
        return Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1")
            .ignoreContentType(true)
    }
}