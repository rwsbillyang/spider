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


class BaiDuTest: SpiderTestBase()  {

   // @Test
    fun test1(){
        newsTest("http://baijiahao.baidu.com/s?id=1692558786334703012")
    }

   // @Test
    fun test2(){
        newsTest("https://mbd.baidu.com/newspage/data/landingshare?p_from=7&n_type=-1&context=%7B%22nid%22%3A%22news_9367297194102527383%22%7D")
    }

   // @Test
    fun test3(){
        //我国还要坚持食盐加碘政策吗？全国地方病防治专家回应
        newsTest("https://news.baidu.com/news#/detail/8569134799515423156")
    }

    //@Test
    fun test4(){
        newsTest("https://news.baidu.com/news#/detail/9083552498780951387")
    }
}