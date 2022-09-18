/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-09-17 11:59
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

package com.github.rwsbillyang.spider.utils

import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern


object HtmlImgUtil {
    private val regxpForHtml = "<([^>]*)>" // 过滤所有以<开头以>结尾的标签
    private val regxpForImgTag = "<\\s*img\\s+([^>]*)\\s*>" // 找出IMG标签
    private val regxpForImaTagSrcAttrib = "src=\"([^\"]+)\"" // 找出IMG标签的SRC属性

    //String regxp = "<\\s*" + tag + "\\s+([^>]*)\\s*>";   红色的 tag 是动态的变量（指定标签）
    //String regxp = "<\\s*" + tag + "\\s+([^>]*)\\s*>";   红色的 tag 是动态的变量（指定标签）

    val PATTERN: Pattern = Pattern.compile(
        "<img\\s+(?:[^>]*)src\\s*=\\s*([^>]+)",
        Pattern.CASE_INSENSITIVE or Pattern.MULTILINE
    )


    /**
     * 获取新闻内容中的第一张图片，即第一个img标签的src属性的值
     * @param htmlContent 通常为新闻内容，如百度编辑器编辑后保存的新闻详情字段，将从中提取出第一个img标签
     * @return 返回第一张图片的路径，即img标签中的src属性值。如果没有图片，返回null
     */
    fun getImageSrc(htmlContent: String?): List<String>? {
        if (htmlContent.isNullOrBlank()) return null
        val matcher: Matcher = PATTERN.matcher(htmlContent)
        val list = ArrayList<String>()
        while (matcher.find()) {
            val group: String = matcher.group(1) ?: continue
            //   这里可能还需要更复杂的判断,用以处理src="...."内的一些转义符
            if (group.startsWith("'")) {
                list.add(group.substring(1, group.indexOf("'", 1)))
            } else if (group.startsWith("\"")) {
                list.add(group.substring(1, group.indexOf("\"", 1)))
            } else {
                list.add(group.split("\\s").toTypedArray()[0])
            }
        }
        return list
    }


    fun getImageSrc2(htmlCode: String?): List<String?>? {
        if (htmlCode.isNullOrBlank()) return null
        val imageSrcList: MutableList<String?> = ArrayList()
        val p: Pattern = Pattern.compile(
            "<img\\b[^>]*\\bsrc\\b\\s*=\\s*('|\")?([^'\"\n\r>]+(\\.jpg|\\.bmp|\\.eps|\\.gif|\\.mif|\\.miff|\\.png|\\.tif|\\.tiff|\\.svg|\\.wmf|\\.jpe|\\.jpeg|\\.dib|\\.ico|\\.tga|\\.cut|\\.pic)\\b)[^>]*>",
            Pattern.CASE_INSENSITIVE
        )
        val m: Matcher = p.matcher(htmlCode)
        var quote: String?
        var src: String?
        while (m.find()) {
            quote = m.group(1)

            // src=https://sms.reyo.cn:443/temp/screenshot/zY9Ur-KcyY6-2fVB1-1FSH4.png
            src =
                if (quote == null || quote.trim { it <= ' ' }.isEmpty()) m.group(2).split("\\s+").get(0) else m.group(
                    2
                )
            imageSrcList.add(src)
        }
        return imageSrcList
    }
}