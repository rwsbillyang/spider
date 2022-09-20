package com.github.rwsbillyang.spider.test




import com.github.rwsbillyang.spider.news.ToutiaoSpider


import kotlin.test.Test



class TouTiaoTest: SpiderTestBase() {
    override val spider = ToutiaoSpider(driverPath)

    @Test
    fun test1(){
        newsTest("https://www.toutiao.com/article/7132394799076311584/?app=news_article&timestamp=1663290936&use_new_style=1&req_id=202209160915350101960350661A25B143&group_id=7132394799076311584&share_token=63da7242-3e4d-48d6-8676-42d6ca662a06&tt_from=copy_link&utm_source=copy_link&utm_medium=toutiao_android&utm_campaign=client_share&source=m_redirect&wid=1663291414789")
    }
    @Test
    fun test2(){
        newsTest("https://m.toutiaocdn.net/a6940884912362865183/?app=news_article&is_hit_share_recommend=0&share_token=a146bed7-329d-409d-8f54-77be583cd85b&tt_from=copy_link&utm_source=copy_link&utm_medium=toutiao_android&utm_campaign=client_share")
    }

    @Test
    fun test3(){
        newsTest("https://www.toutiao.com/a6932388257619657228/")
    }
    @Test
    fun test4(){
        newsTest("https://m.toutiao.com/is/eLUafUy/")
    }
    @Test
    fun test5(){
        newsTest("https://m.toutiaocdn.com/i6930596509947871747/?app=news_article_lite&timestamp=1614248713&use_new_style=1&req_id=202102251825130101351551474403EE48&group_id=6930596509947871747&share_token=c7d8f7b1-122d-43b9-a777-e335a8288176")
    }

    @Test
    fun test11(){
        //https://m.toutiao.com/video/6911200660990263821/
        videoTest("https://m.toutiaoimg.cn/i6911200660990263821/")
    }

    @Test
    fun test12(){
        //https://m.toutiao.com/video/6937958915501982221/?app=news_article&is_hit_share_recommend=0&share_token=dc823786-e7de-4e7f-a590-76a5a14d6305&tt_from=copy_link&utm_source=copy_link&utm_medium=toutiao_android&utm_campaign=client_share
        videoTest("https://m.toutiao.com/is/eLAFUoL/")
    }
    @Test
    fun test13(){
        //https://m.toutiao.com/video/6919011618969879044/?app=news_article&is_hit_share_recommend=0&share_token=a42c1491-d372-4c5d-b43a-1dbef0a69f33&tt_from=copy_link&utm_source=copy_link&utm_medium=toutiao_android&utm_campaign=client_share
        videoTest("https://m.toutiao.com/is/eLUaQgd/")
    }


    @Test
    fun test14(){
        //https://m.toutiao.com/video/6926884519626834436/?app=news_article&is_hit_share_recommend=0&share_token=1782d238-a298-431e-b95c-c2e978e0876d&tt_from=copy_link&utm_source=copy_link&utm_medium=toutiao_android&utm_campaign=client_share
        videoTest("https://m.toutiao.com/is/eLUQAbF/")
    }
    @Test
    fun test15(){
        videoTest("https://m.toutiaoimg.cn/i6911200660990263821/")
    }

    @Test
    fun test16(){
        videoTest("https://m.toutiaoimg.cn/a6904293095572144644/?app=news_article_lite&is_hit_share_recommend=0&share_token=8393acac-4faa-44b8-8fb4-9d130b6f2fc5")
    }

    @Test
    fun test17(){
        videoTest("https://www.toutiao.com/video/7144673630465884712/?wxshare_count=1&tt_from=weixin&utm_source=weixin&utm_medium=toutiao_android&utm_campaign=client_share&share_token=a298388e-f898-47c6-a35f-d4cac91f5f15&source=m_redirect&wid=1663507036625")
    }


     @Test
    fun test21(){
         //https://m.zjurl.cn/question/6720386012503113991/?app=news_article&app_id=13&share_ansid=6937296144544825638&share_token=9c2815fc-ebc7-42e2-b2ce-41d35841aa4f&tt_from=copy_link&utm_source=copy_link&utm_medium=toutiao_android&utm_campaign=client_share
        newsTest("https://m.toutiao.com/is/eLAFjSc/")
    }


    @Test
    fun test31(){
       newsTest("https://www.toutiao.com/w/1744123870313500/?app=news_article&timestamp=1663669973&use_new_style=1&share_token=942703b5-294f-40ba-848a-e357a87ee8a7&tt_from=copy_link&utm_source=copy_link&utm_medium=toutiao_android&utm_campaign=client_share&source=m_redirect&upstream_biz=toutiao_pc&source=m_redirect")
    }
    @Test
    fun test32(){
        newsTest("https://m.toutiao.com/w/1744012349973518?app=news_article&timestamp=1663669973&use_new_style=1&share_token=942703b5-294f-40ba-848a-e357a87ee8a7&tt_from=copy_link&utm_source=copy_link&utm_medium=toutiao_android&utm_campaign=client_share&source=m_redirect&upstream_biz=toutiao_pc&from_gid=1744001366265856&from_page_type=weitoutiao")
    }
}
