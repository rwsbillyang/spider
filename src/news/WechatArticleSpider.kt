/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-02-21 21:13
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

package com.github.rwsbillyang.spider.news

import com.github.rwsbillyang.spider.*

class WechatArticleSpider: PageStreamParser(), ISpider {
    override val regPattern = "http(s)?://mp\\.weixin\\.qq\\.com/\\S+"
    override val errMsg = "非mp.weixin.qq.com开头的文章链接无法上传"

    override val extractRules =
        arrayOf(
            ExtractRule(Spider.TITLE, PrefixMatchRule("<meta property=\"og:title\"", "content=\"", "\"")),
            ExtractRule(Spider.OGURL, PrefixMatchRule("<meta property=\"og:url\"", "content=\"", "\"")),
            ExtractRule(Spider.IMGURL, PrefixMatchRule("<meta property=\"og:image\"", "content=\"", "\"")),
            ExtractRule(
                Spider.BRIEF,
                PrefixMatchRule("<meta property=\"og:description\"", "content=\"", "\"")
            ),

            ExtractRule(Spider.USER, ContainMatchRule("profile_nickname", ">", "<")),
            ExtractRule(Spider.USER2, PrefixMatchRule("d.nick_name", "\"", "\"")),
            ExtractRule(
                Spider.CONTENT, MultiLineRule(
                    ContainRule("id=\"js_content\""),
                    EqualRule("</div>"),
                    ContainRule("<section class=\"cps_inner cps_inner_list js_list_container js_product_container\">"),
                    ContainRule("</section>")
                )
            ),
            ExtractRule(Spider.TAG, PrefixMatchRule("var _ori_article_type", "\"", "\"")),
            //ExtractRule(Spider.LINK, PrefixMatchRule("var msg_link", "\"", "\"")),
        )

    override fun doParse(url: String, map: MutableMap<String, String?>) {
        getPageAndParse(url, map)
        if(map[Spider.LINK] == null) {
            map[Spider.LINK] = url
        }
    }
}
