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

import com.github.rwsbillyang.spider.ISpider


import com.github.rwsbillyang.spider.news.SpiderCM163

import org.junit.Test

 class SpiderCM163Test: SpiderTestBase()  {
    override val spider: ISpider = SpiderCM163()

    @Test
    fun test1(){
        newsTest("https://c.m.163.com/news/a/G3MCJ3SK05199NPP.html")
    }

    @Test
    fun test2(){
        newsTest("https://c.m.163.com/news/a/HHFA77BV000189FH.html")
    }

    @Test
    fun test3(){
        newsTest("https://c.m.163.com/news/a/HHF5GR5000019K82.html")
    }

    @Test
    fun test4(){
        newsTest("https://c.m.163.com/news/a/HGOG88IB0553A9YH.html")
    }
}