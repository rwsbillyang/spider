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
import com.github.rwsbillyang.spider.utils.*


/**
 * 网页html全部在一行，使用LoopMode.ByField
 * @deprecated("头条升级导致不能使用")
 * */
class ToutiaoSpiderOld2 : PageStreamParser(Spider.uas[Spider.UAs_WX], LoopMode.ByField), ISpider {

    override val regPattern = "http(s)?://(m|www)\\.toutiao(cdn)?\\.(com|cn|net)/(a|i|article)\\S+"
    override val errMsg =
        "只支持头条文章和微头条 链接中有这些字符：toutiao.com 或 toutiaocdn.com"

    override val extractRules =
        arrayOf(
            ExtractRule(Spider.TITLE, ContainMatchRule("<h1>","<h1>","</h1>")),
            ExtractRule(Spider.USER, ContainMatchRule("\"Person\",\"name\":", "\"", "\""), StartIndexHint.AfterMatchStrIndex),
            ExtractRule(Spider.BRIEF,ContainMatchRule("<meta name=\"description\"","content=\"","\""), StartIndexHint.AfterMatchStrIndex),
            ExtractRule(Spider.CONTENT, PrefixMatchRule("<article ",">","</article>"), StartIndexHint.AfterMatchStrIndex)
        )

    override fun doParse(url: String): Map<String, String?> {
        val resultMap = mutableMapOf<String, String?>()
        getPageAndParse(url, resultMap)
        if(resultMap[Spider.LINK] == null) {
            resultMap[Spider.LINK] = url
        }
        return resultMap
    }

}