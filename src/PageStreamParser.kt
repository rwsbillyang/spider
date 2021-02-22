/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-02-21 20:56
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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.net.URLConnection

/**
 * 抽取规则
 * @param key 抽取得到的值，放到map中，用到的key
 * @param rule 抽取规则 分别为前缀匹配、包含匹配、后缀匹配，以及多行匹配；
 * */
class ExtractRule(
    val key: String,
    val rule: Rule
)
/**
 * 提取匹配模式种类，支持前缀匹配、包含、后缀匹配、完全相等 TODO：未来支持正则表达式
 * @property Prefix 用于找到以某种字符串开头的行
 * @property Contain 用于找到包含某种字符串结尾的行
 * @property Suffix 用于找到以某种字符串结束的行
 * @property Equal 用于找到完全是字符串结束的行
 * @property MultiLine 多行模式
 * */
enum class MatchPattern{
    Prefix, Contain, Suffix, Equal, MultiLine
}

open class Rule(
    open val matchPattern: MatchPattern
)
/**
 * 规则： 得到以 matchPattern 方式匹配 matchStr 的单行，得到 start 和 end 之间的字符串
 * @param matchPattern 匹配模式
 * @param matchStr 特征符
 * */
sealed class SingleLineRule(
    override val matchPattern: MatchPattern,
    open val matchStr: String,
    open val start: String,
    open val end: String
): Rule(matchPattern)


/**
 * @param start 开始特征符
 * @param end 结束特征符
 * */
class PrefixMatchRule(
    override val matchStr: String,
    override  val start: String,
    override  val end: String
): SingleLineRule(MatchPattern.Prefix, matchStr, start, end)
/**
 * @param start 开始特征符
 * @param end 结束特征符
 * */
class ContainMatchRule(
    override val matchStr: String,
    override  val start: String,
    override  val end: String
): SingleLineRule(MatchPattern.Contain, matchStr, start, end)

/**
 * @param start 开始特征符
 * @param end 结束特征符
 * */
class SuffixMatchRule(
    override val matchStr: String,
    override val start: String,
    override val end: String
): SingleLineRule(MatchPattern.Suffix, matchStr, start, end)

/**
 * 用于多行判断
 * */
sealed class LineRule(override val matchPattern: MatchPattern, open val matchStr: String): Rule(matchPattern)
class EqualRule(override val matchStr: String): LineRule(MatchPattern.Equal, matchStr)
class PrefixRule(override val matchStr: String): LineRule(MatchPattern.Prefix, matchStr)
class ContainRule(override val matchStr: String): LineRule(MatchPattern.Contain, matchStr)
class SuffixRule(override val matchStr: String): LineRule(MatchPattern.Suffix, matchStr)
/**
 * 得到以 startLineRule 开始和 endLineRule 结束之间的所有行，
 * 但需排除掉 excludeBegin 开头和 excludeEnd 两种标签之间的行（若它们不空的话）
 * */
class MultiLineRule(
    val startLineRule: LineRule,// 不包括该行
    val endLineRule: LineRule, //不包括该行
    val excludeBegin: LineRule? = null, //该行及以后的内容将被移除
    val excludeEnd: LineRule? = null //该行及以前的内容将被移除
): Rule(MatchPattern.MultiLine)

object ParseLineResult{
    const val Ignore = 0
    const val SingLineOK = 1
    const val MultiLineStart = 2 //多行开始标志
}

/**
 * 对请求获取的网页字符串流进行处理，逐行检查获取所需的值
 * */
abstract class PageStreamParser {
    val log: Logger = LoggerFactory.getLogger("PageStreamParser")

    abstract val extractRules: Array<ExtractRule>

    fun getPageAndParse(url: String, map: MutableMap<String, String?>) {
        parseStream(url, getPageInputStream(url), map)
    }

    private fun getPageInputStream(url: String): InputStream? {
        log.info("parse url=$url")
        try {
            //建立请求链接
            val conn: URLConnection = URL(url).openConnection()
            conn.doInput = true
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.defaultUseCaches = true
            conn.setRequestProperty("User-agent", Spider.UAs[0])
            conn.setRequestProperty("Charset", "UTF-8")
            conn.connect() //本方法不会自动重连

            return conn.getInputStream() ?: return null
        } catch (e: FileNotFoundException) {
            log.warn("FileNotFoundException, url=$url")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }



    private fun parseStream(url: String, `is`: InputStream?, map: MutableMap<String, String?>) {
        if (`is` == null) {
            log.warn("InputStream is null")
            map[Spider.RET] = Spider.KO
            map[Spider.MSG] = "获取内容失败"
            return
        }

        try {
            val `in` = BufferedReader(InputStreamReader(`is`, "UTF-8"))
            var line: String? = null
            var count = 0
            var ret: Int

            var keyRule: ExtractRule? = null

            var multiLineRule: MultiLineRule? = null
            var excludeEnd: LineRule? = null
            var excludeBegin: LineRule? = null
            var inMultiLineMode = false
            var inMultilineExclusiveMode = false
            val sb = StringBuilder()

            val flagArray = IntArray(extractRules.size) //finding result flag results array, initialized 0

            // 循环读取流
            LoopWhile@ while (`in`.readLine()?.also { line = it } != null) {
                line = line?.trim()
                if(line.isNullOrBlank()) continue
                //log.info("after trim,line="+line)

                if (!inMultiLineMode) {
                    LoopFor@ for (i in extractRules.indices) {
                        if (flagArray[i] == 1) continue@LoopFor //has found,skip

                        keyRule = extractRules[i]
                        ret = parseLine(line!!, map, keyRule)

                        if (ret == ParseLineResult.Ignore) {
                            continue@LoopFor
                        } else if (ret == ParseLineResult.SingLineOK) {
                            flagArray[i] = 1
                            count++
                            continue@LoopWhile
                        } else if (ret == ParseLineResult.MultiLineStart) {
                            log.info("enter multiline mode...")
                            flagArray[i] = 1

                            inMultiLineMode = true //下一次的读取line循环将处于多行模式
                            multiLineRule = keyRule.rule as? MultiLineRule
                            excludeEnd = multiLineRule?.excludeEnd
                            excludeBegin = multiLineRule?.excludeBegin

                            continue@LoopWhile
                        } else {
                            log.warn("NOT support result=$ret")
                            continue@LoopFor
                        }
                    }
                } else {//已经是多行模式，若是的话表示需要判断多行结束标志；否则需要判断多行是否开始
                    if (inMultilineExclusiveMode) {
                        if (excludeEnd != null && isMatch(line!!, excludeEnd)) {
                            inMultilineExclusiveMode = false
                            line = line!!.substringAfter(excludeEnd.matchStr).trim() //删除特征符以前的，保留以后的
                            if (!line.isNullOrBlank()) sb.append(line)
                            continue
                        } else continue
                    } else {
                        if (isMatch(line!!, multiLineRule!!.endLineRule)) {
                            inMultiLineMode = false
                            log.info("......exit multiline mode")
                            map[keyRule!!.key] = sb.toString()
                            count++
                            continue
                        }else{
                            if (excludeBegin != null && isMatch(line!!, excludeBegin)) {
                                inMultilineExclusiveMode = true
                                line = line!!.substringBefore(excludeBegin.matchStr).trim()
                                if (!line.isNullOrBlank()) sb.append(line)
                                continue
                            } else {
                                sb.append(line)
                                continue
                            }
                        }
                    }
                }
            } //while-loop

            //still inMultiLineMode
            if (inMultiLineMode) {
                log.warn("still in MultiLine Mode after reach end, url=$url")
                map[keyRule!!.key] = sb.toString()
                count++
            }

            if (count == extractRules.size) {
                map[Spider.RET] = Spider.OK
                map[Spider.MSG] = "恭喜，解析成功，请编辑保存！"
            } else {
                log.warn("got results number:" + count + ",less than " + extractRules.size + ",url=" + url)
                map[Spider.RET] = Spider.OK
                map[Spider.MSG] = "得到部分解析结果！请耐心等候系统升级！"
            }
            `in`.close()
            `is`.close() //关闭InputStream

            return
        } catch (e: Exception) {
            e.printStackTrace()
            log.error("caused Exception, the url=$url")
            map[Spider.RET] = Spider.KO
            map[Spider.MSG] = "获取内容时网络超时，请重试"
            return
        }
    }
    /**
     * 抽取行中的值，结果放入map中，只对单行模式使用，当需要抽取的值处在多行时，需另行处理。
     *
     * @param line 待抽取的行
     * @param map 存放抽取结果
     * @param keyRule 使用的规则
     * @return 不是匹配的行，返回0，是匹配的单行，抽取后返回1，多行模式返回2
     */
    private fun parseLine(line: String, map: MutableMap<String, String?>,  keyRule: ExtractRule): Int {
        val isTargetLine: Boolean
        when (val rule = keyRule.rule) {
            is PrefixMatchRule -> {
                isTargetLine = line.startsWith(rule.matchStr)
                return if(isTargetLine){
                    extractValue(line, rule.start, rule.end)?.let {  map [keyRule.key] = it }
                    ParseLineResult.SingLineOK
                }else
                    ParseLineResult.Ignore
            }
            is ContainMatchRule -> {
                isTargetLine = line.contains(rule.matchStr)
                return if(isTargetLine){
                    extractValue(line, rule.start, rule.end)?.let {  map [keyRule.key] = it }
                    ParseLineResult.SingLineOK
                }else
                    ParseLineResult.Ignore
            }
            is SuffixMatchRule -> {
                isTargetLine = line.endsWith(rule.matchStr)
                return if(isTargetLine){
                    extractValue(line, rule.start, rule.end)?.let {  map [keyRule.key] = it }
                    ParseLineResult.SingLineOK
                }else
                    ParseLineResult.Ignore
            }
            is MultiLineRule -> {
                return if(isMatch(line, rule.startLineRule)) ParseLineResult.MultiLineStart else ParseLineResult.Ignore
            }
            else -> {
                log.warn("should not rule: ${rule.matchPattern}")
                return ParseLineResult.Ignore
            }
        }
    }

    private fun isMatch(line: String, rule: LineRule) = when (rule) {
        is PrefixRule -> line.startsWith(rule.matchStr)
        is ContainRule -> line.contains(rule.matchStr)
        is SuffixRule -> line.endsWith(rule.matchStr)
        is EqualRule -> line.equals(rule.matchStr, ignoreCase = true)
    }

    private fun extractValue(line: String, start: String, end: String): String?{
        val startIndex = line.indexOf(start) + start.length
        val endIndex = line.indexOf(end, startIndex)
        return if (startIndex < endIndex) {
            line.substring(startIndex, endIndex)
        } else {
            // log.warn("Not found value for key=${keyRule.key},line=" + line)
            null
        }
    }
}