/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-09-16 16:14
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

package com.github.rwsbillyang.spider.test

import org.junit.Test

class Spider3G163Test: SpiderTestBase()  {

    @Test
    fun test1(){
        newsTest("https://3g.163.com/news/article/G3K55190000189FH.html?clickfrom=index2018_news_newslist#offset=0")
    }

    @Test
    fun test2(){
        newsTest("https://3g.163.com/news/article/G3RFDHQF000189FH.html")
    }

    @Test
    fun test3(){
        newsTest("https://3g.163.com/news/article/DB8SPSIU0001875P.html")
    }

    @Test
    fun test4(){
        newsTest("https://3g.163.com/dy/article/HHF8N86G053469LG.html")
    }
}