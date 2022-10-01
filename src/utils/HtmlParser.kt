/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-09-17 12:00
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



import com.github.rwsbillyang.spider.Spider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*

/**
 * 抽取规则
 * @param key 抽取得到的值，放到map中，用到的key
 * @param rule 抽取规则 分别为前缀匹配、包含匹配、后缀匹配，以及多行匹配；
 * */
class ExtractRule(
    val key: String,
    val rule: Rule,
    val startIndexHint:StartIndexHint = StartIndexHint.None
)
enum class StartIndexHint{
    BeforeMatchStrIndex, AfterMatchStrIndex, None
}
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
class EqualRule(override val matchStr: String)
    : LineRule(MatchPattern.Equal, matchStr)
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
 * 请求返回的html文档可能有很多行，需要提取的数据分布在各行上，遍历各行进行解析提取
 * 还有一种可能是html无换行，全部在一行，这时提取需要对各字段进行循环，都对该行进行解析提取
 * */
enum class LoopMode{
    ByLine, ByField
}


/**
 * 对请求获取的网页字符串流进行处理，逐行检查获取所需的值
 * */
class HtmlParser(private val loopMode:LoopMode = LoopMode.ByLine) {
    private val log: Logger = LoggerFactory.getLogger("HtmlParser")

    private var currentLineIndex: Int = 0 //当前待解析的行
    private var extractCount: Int = 0 //已解析出的结果计数
    private var loopTime: Int = 0 //ByField时会计数，用于抽取不到值时循环计数，及时退出循环


    fun parse(html: String, extractRules: Array<ExtractRule>, resultMap: MutableMap<String, String?>) {
        if (html.isBlank()) {
            log.warn("InputStream is null")
            resultMap[Spider.RET] = Spider.KO
            resultMap[Spider.MSG] = "获取内容失败"
            return
        }
        val lines = html.split("\n")
        log.info("got lines.size=${lines.size}")
        try {
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
            LoopWhile@ while(continueLoop(lines, extractRules)) {
                val currentLine = lines[currentLineIndex++].trim()
                if(currentLine.isBlank()) continue

                //log.info("line="+line)

                if (!inMultiLineMode) {
                    LoopFor@ for (i in extractRules.indices) {
                        if (flagArray[i] == 1) continue@LoopFor //has found,skip

                        keyRule = extractRules[i]
                        ret = parseLine(currentLine, resultMap, keyRule)

                        if (ret == ParseLineResult.Ignore) {
                            continue@LoopFor
                        } else if (ret == ParseLineResult.SingLineOK) {
                            flagArray[i] = 1
                            extractCount++
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
                        if (excludeEnd != null && isMatch(currentLine, excludeEnd)) {
                            inMultilineExclusiveMode = false
                            val line = currentLine.substringAfter(excludeEnd.matchStr).trim() //删除特征符以前的，保留以后的
                            if (!line.isNullOrBlank()) sb.append(line)
                            continue
                        } else continue
                    } else {
                        if (isMatch(currentLine, multiLineRule!!.endLineRule)) {
                            inMultiLineMode = false
                            log.info("......exit multiline mode")
                            resultMap[keyRule!!.key] = sb.toString()
                            extractCount++
                            continue
                        }else{
                            if (excludeBegin != null && isMatch(currentLine, excludeBegin)) {
                                inMultilineExclusiveMode = true
                                val line = currentLine.substringBefore(excludeBegin.matchStr).trim()
                                if (!line.isNullOrBlank()) sb.append(line)
                                continue
                            } else {
                                sb.append(currentLine)
                                continue
                            }
                        }
                    }
                }
            } //while-loop

            //still inMultiLineMode
            if (inMultiLineMode) {
                log.warn("still in MultiLine Mode after reach end")
                resultMap[keyRule!!.key] = sb.toString()
                extractCount++
            }

            if (extractCount == extractRules.size) {
                resultMap[Spider.RET] = Spider.OK
                resultMap[Spider.MSG] = "恭喜，解析成功，请编辑保存！"
            } else {
                log.warn("got results number:" + extractCount + ",less than " + extractRules.size)
                resultMap[Spider.RET] = Spider.OK
                resultMap[Spider.MSG] = "得到部分解析结果！请耐心等候系统升级！"
            }

            return
        } catch (e: IOException) {
            resultMap[Spider.RET] = Spider.KO
            resultMap[Spider.MSG] = "获取内容时网络超时，请重试"
            return
        }
    }

    /**
     * 默认实现适用于html文档有很多行，需要提取的数据分布在各行上，遍历各行进行解析提取
     * 还有一种可能是html无换行，全部在一行，这时提取需要对各字段进行循环，进行解析提取
     * */
    private fun continueLoop(lines: List<String>, extractRules: Array<ExtractRule>): Boolean{
        //没有内容了，退出循环
        if(currentLineIndex >= lines.size) return false

        //已经全部得到值，无需再进行解析，退出循环
        if(extractCount >= extractRules.size) return false

        //log.info("currentLineIndex=$currentLineIndex")
        return if(LoopMode.ByField == loopMode){
            loopTime++
            //只有一行时，遍历次数够了，也退出循环
            loopTime <= extractRules.size
        }else
            true
    }

    /**
     * 抽取行中的值，结果放入map中，只对单行模式使用，当需要抽取的值处在多行时，需另行处理。
     *
     * @param line 待抽取的行
     * @param resultMap 存放抽取结果
     * @param keyRule 使用的规则
     * @return 不是匹配的行，返回0，是匹配的单行，抽取后返回1，多行模式返回2
     */
    private fun parseLine(line: String, resultMap: MutableMap<String, String?>,  keyRule: ExtractRule): Int {
        val isTargetLine: Boolean
        when (val rule = keyRule.rule) {
            is PrefixMatchRule -> {
                isTargetLine = line.startsWith(rule.matchStr)
                return if(isTargetLine){
                    extractValue(line, rule.start, rule.end, rule.matchStr, keyRule.startIndexHint)?.let {  resultMap [keyRule.key] = it }
                    ParseLineResult.SingLineOK
                }else
                    ParseLineResult.Ignore
            }
            is ContainMatchRule -> {
                isTargetLine = line.contains(rule.matchStr)
                return if(isTargetLine){
                    extractValue(line, rule.start, rule.end, rule.matchStr, keyRule.startIndexHint)?.let {  resultMap [keyRule.key] = it }
                    ParseLineResult.SingLineOK
                }else
                    ParseLineResult.Ignore
            }
            is SuffixMatchRule -> {
                isTargetLine = line.endsWith(rule.matchStr)
                return if(isTargetLine){
                    extractValue(line, rule.start, rule.end, rule.matchStr, keyRule.startIndexHint)?.let {  resultMap [keyRule.key] = it }
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

    private fun extractValue(line: String, start: String, end: String, matchStr:String, startIndexHint:StartIndexHint): String?{
        val index = when(startIndexHint){
            StartIndexHint.AfterMatchStrIndex -> line.indexOf(matchStr) + matchStr.length
            StartIndexHint.BeforeMatchStrIndex, StartIndexHint.None -> 0
        }
        val startIndex = line.indexOf(start, index) + start.length
        val endIndex = line.indexOf(end, startIndex)
        log.info("matchStr=${matchStr},startIndex=$startIndex, endIndex=$endIndex")
        return if (startIndex < endIndex) {
            line.substring(startIndex, endIndex)
        } else {
            log.warn("Not found value for matchStr=${matchStr},startIndex=$startIndex, endIndex=$endIndex")
            null
        }
    }
}