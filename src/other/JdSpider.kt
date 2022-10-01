/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-09-25 21:13
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

package com.github.rwsbillyang.spider.other


import com.github.rwsbillyang.spider.ChromeDriverServiceWrapper
import com.github.rwsbillyang.spider.Spider
import com.github.rwsbillyang.spider.WebDriverClient
import kotlinx.serialization.Serializable
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

import java.time.Duration

fun String.between(startDelimiter: String, endDelimiter: String): String? {
    val startIndex = this.indexOf(startDelimiter)
    if (startIndex < 0) return null
    val start = startIndex + startDelimiter.length
    val endIndex = this.indexOf(endDelimiter, start)
    val end = if (endIndex < 0) this.length else endIndex
    return this.substring(start, end)
}

@Serializable
class JdGoods(
    var title: String? = null,
    var brand: String? = null,
    var intro: String? = null,
    var mobileIntro: String? = null,
    val skuList: MutableList<SkuInfo> = mutableListOf()
)

@Serializable
class SkuInfo(
    val id: String? = null,
    val name: String? = null,
    var price: String? = null,
    var images: List<String>? = null
)


class JdSpiderPc(private val goods: JdGoods, uaIndex: Int = Spider.UAs_PC) : WebDriverClient(uaIndex, true) {
    override val regPattern: String = "http(s)?://item\\.jd\\.com/\\S+"
    override val errMsg: String = "非JD网址"

    override fun extract(url: String, map: MutableMap<String, String?>) {
        val contentDiv: WebElement = WebDriverWait(webDriver, Duration.ofSeconds(ChromeDriverServiceWrapper.timeOut))
            .until { webDriver.findElement(By.cssSelector("div#J-detail-content")) }

        goods.title = webDriver.title
        goods.intro = contentDiv.getAttribute("innerHTML")
            .replace(Regex("\\s+src=\"\\S+?\""), "")
            .replace("class=\"ELazy-loading\"", "")
            .replace("data-lazyload", "src")

        goods.brand =
            webDriver.findElements(By.cssSelector("ul#parameter-brand>li")).firstOrNull()?.getAttribute("title")
        webDriver.findElements(By.ByCssSelector("div#choose-attr-1>div.dd>div.item")).forEach {
            //println(it.getAttribute("outerHTML"))
            //it.click() //点击后页面刷新 dom不再是原来的dom，导致报错：org.openqa.selenium.StaleElementReferenceException: stale element reference: element is not attached to the page document
            //Thread.sleep(3000)
            //val price = webDriver.findElement(By.ByCssSelector("div.dd>span.p-price>span.price"))?.text

            goods.skuList.add(SkuInfo(it.getAttribute("data-sku"), it.getAttribute("title")))
        }
    }
}

//https://item.m.jd.com/product/10026501135667.html
class JdSpiderMobile(private val goods: JdGoods, uaIndex: Int = Spider.UAs_Mobile) : WebDriverClient(uaIndex, true) {
    override val regPattern: String = "http(s)?://item\\.m\\.jd\\.com/\\S+"
    override val errMsg: String = "非JD mobile网址"

    override fun extract(url: String, map: MutableMap<String, String?>) {
        //webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10))
        Thread.sleep(5000)
        log.info("readyState=" + webDriver.executeScript("return document.readyState").toString())

        webDriver.executeScript("window.scrollBy(0,200)") //目的让展示出上面的tab：点击"详情"

        webDriver.findElement(By.linkText("详情"))
            .click()//点击"详情" //webDriver.findElements(By.cssSelector("div#detailAnchor>nav>a"))[2].click()
        val wait = WebDriverWait(webDriver, Duration.ofSeconds(ChromeDriverServiceWrapper.timeOut))

        val contentDiv = wait.until{webDriver.findElement(By.cssSelector("div#commDesc"))}
        //val contentDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div#commDesc")))//将得到的正常加载的img及其src

        goods.mobileIntro = contentDiv.getAttribute("innerHTML")
        //.replace(Regex("\\s+src=\"\\S+?\""), "")
        //.replace("item_init_src","src")


        webDriver.executeScript("window.scrollTo(0,0)")//回到顶部
        Thread.sleep(500)
        webDriver.executeScript("window.scrollBy(0,200)")//再往下移动显示skuWindow
        Thread.sleep(500)

        val skuWindow: WebElement = webDriver.findElement(By.cssSelector("div#skuWindow")) //elementToBeClickable
        wait.until(ExpectedConditions.elementToBeClickable(skuWindow))
        skuWindow.click()
        Thread.sleep(1000)
        val skuPop1 = webDriver.findElement(By.cssSelector("div#skuPop1"))
        if (goods.skuList.isEmpty()) {//如果没有信息列表的话，获取sku name，形成列表
            skuPop1.findElements(By.cssSelector("span.item")).forEach {
                goods.skuList.add(SkuInfo(null, it.text))
            }
        }

        val resultMap = mutableMapOf<Int, Boolean>()
        val currentIndex = getActiveIndex()
        if (currentIndex > 0) {
            getPrice(currentIndex)
            getSlides(currentIndex)
            resultMap[currentIndex] = true

            if(!goods.title.isNullOrEmpty()){
                goods.skuList[currentIndex].name?.let { goods.title = goods.title?.substringBefore(it) }
            }
        }

        goods.skuList.forEachIndexed { index, _ ->
            if (resultMap[index] == null) {
                switchToSku(index, wait)
                getPrice(index)
                getSlides(index)
                resultMap[index] = true
            }
        }
    }

    private fun getActiveIndex(): Int {
        webDriver.findElements(By.cssSelector("div#skuPop1>span")).forEachIndexed { index, webElement ->
            if (webElement.getAttribute("outerHTML").contains("active")) return index
        }
        return -1
    }

    private fun switchToSku(index: Int, wait: WebDriverWait) {
        val sku = webDriver.findElements(By.cssSelector("div#skuPop1>span.item"))[index]
        //log.info("to switch index=$index: ${sku.getAttribute("outerHTML")}")
        wait.until(ExpectedConditions.elementToBeClickable(sku))
        sku.click()
        Thread.sleep(3000)
    }

    private fun getPrice(index: Int) {
        val div = webDriver.findElement(By.cssSelector("div#popupMain>div.header>div")).getAttribute("innerHTML")
        val price = div.between("<em>", "</em>")
        log.info("getPrice index=$index, price=$price")
        goods.skuList[index].price = price
    }

    private fun getSlides(index: Int) {
       // val slides = webDriver.findElement(By.cssSelector("div#loopImgDiv>div.inner"))
        //log.info("before move, getSlides index=$index, slides=${slides.getAttribute("innerHTML")}")

        //not work, use back_src instead
//        val li = slides.findElements(By.cssSelector("ul>li"))
//        val size = li.size
//        for(i in 0 until size-1){
//            move(li[i])
//        }

        goods.skuList[index].images = webDriver.findElements(By.cssSelector("div#loopImgDiv>div.inner>ul>li>img"))
            .map {
                val backSrc = it.getAttribute("back_src")
                if (backSrc.isNullOrEmpty()) it.getAttribute("src") else backSrc
            }
    }

    /**
     * 滑动target元素，向左时left为-1，right则为1
     * */
    private fun move(target: WebElement, left: Int = -1) {
        Actions(webDriver)
            .moveToElement(target)
            .dragAndDropBy(target, (target.rect.x / 2 - 10) * left, 0)
            .perform()

        Thread.sleep(3000)
    }
}


object JdSpider {
    fun parse(id: String): JdGoods {
        val goods = JdGoods()
        JdSpiderPc(goods).doParse("https://item.jd.com/$id.html")
        Thread.sleep(1000)
        JdSpiderMobile(goods).doParse("https://item.m.jd.com/product/$id.html")

        return goods
    }
}

fun main() {
    //https://item.m.jd.com/product/10026501135667.html
    //val g = JdSpider.parse("10026501135667")
    //println(Json { prettyPrint = true }.encodeToString(g))

//    val str = "<p><img item_init_src=\"//img10.360buyimg.com/imgzone/jfs/t1/152220/31/14227/107152/5ffc3949E9bd21aba/95fd239894de25fc.jpg!q70.dpg.webp\" src=\"//wq.360buyimg.com/fd/h5/base/detail/images/transparent_a38f0a03.png\"><img item_init_src=\"//img12.360buyimg.com/imgzone/jfs/t1/168708/1/3202/186740/6005357aEd99cb2ff/142245d7ea50feab.jpg!q70.dpg.webp\" src=\"//wq.360buyimg.com/fd/h5/base/detail/images/transparent_a38f0a03.png\"><img item_init_src=\"//img14.360buyimg.com/imgzone/jfs/t1/160119/23/3751/172034/6005357dE36120859/fa64678dc60241de.jpg!q70.dpg.webp\" src=\"//wq.360buyimg.com/fd/h5/base/detail/images/transparent_a38f0a03.png\"><img item_init_src=\"//img14.360buyimg.com/imgzone/jfs/t1/160248/33/3957/154646/60053580Eeb4e46cb/fb293b81b07d725a.jpg!q70.dpg.webp\" src=\"//wq.360buyimg.com/fd/h5/base/detail/images/transparent_a38f0a03.png\"><img item_init_src=\"//img20.360buyimg.com/imgzone/jfs/t1/161430/24/3293/109621/60053583Ec977d16e/e5567b9da976ffb6.jpg!q70.dpg.webp\" src=\"//wq.360buyimg.com/fd/h5/base/detail/images/transparent_a38f0a03.png\"><img item_init_src=\"//img30.360buyimg.com/popWareDetail/jfs/t1/158338/33/3848/137546/6005359dEfc6e54eb/0e879288fc4ca602.jpg!q70.dpg.webp\" src=\"//wq.360buyimg.com/fd/h5/base/detail/images/transparent_a38f0a03.png\"><img item_init_src=\"//img30.360buyimg.com/popWareDetail/jfs/t1/154556/22/15010/169161/600535a0E4bdba2d4/9de90232335de737.jpg!q70.dpg.webp\" src=\"//wq.360buyimg.com/fd/h5/base/detail/images/transparent_a38f0a03.png\"><img item_init_src=\"//img30.360buyimg.com/popWareDetail/jfs/t1/161943/35/3329/126048/600535a5E12144b2d/44ac287a096eac16.jpg!q70.dpg.webp\" src=\"//wq.360buyimg.com/fd/h5/base/detail/images/transparent_a38f0a03.png\"><img item_init_src=\"//img30.360buyimg.com/popWareDetail/jfs/t1/164012/31/3192/115635/600535a8E260aba7c/b15be526f4c39894.jpg!q70.dpg.webp\" src=\"//wq.360buyimg.com/fd/h5/base/detail/images/transparent_a38f0a03.png\"><img item_init_src=\"//img30.360buyimg.com/popWareDetail/jfs/t1/156963/35/6455/148034/600535acE71b28440/33f51f4222efb309.jpg!q70.dpg.webp\" src=\"//wq.360buyimg.com/fd/h5/base/detail/images/transparent_a38f0a03.png\"><img item_init_src=\"//img30.360buyimg.com/popWareDetail/jfs/t1/153805/21/15116/132107/600535afE1919fcbc/5c57c6f49b737019.jpg!q70.dpg.webp\" src=\"//wq.360buyimg.com/fd/h5/base/detail/images/transparent_a38f0a03.png\"><img item_init_src=\"//img30.360buyimg.com/popWareDetail/jfs/t1/161476/1/3215/173956/600535b2E3737a485/bf35f1cfcc0e32bd.jpg!q70.dpg.webp\" src=\"//wq.360buyimg.com/fd/h5/base/detail/images/transparent_a38f0a03.png\"><img item_init_src=\"//img30.360buyimg.com/popWareDetail/jfs/t1/171030/15/3237/183474/600535b5Ee06cb851/f656618be54f41ed.jpg!q70.dpg.webp\" src=\"//wq.360buyimg.com/fd/h5/base/detail/images/transparent_a38f0a03.png\"><img item_init_src=\"//img30.360buyimg.com/popWareDetail/jfs/t1/158243/28/3900/164267/600535b8E7e09a5f0/6d40261d3fcc9223.jpg!q70.dpg.webp\" src=\"//wq.360buyimg.com/fd/h5/base/detail/images/transparent_a38f0a03.png\"><img item_init_src=\"//img30.360buyimg.com/popWareDetail/jfs/t1/87503/28/22816/105718/62047f26E240ae387/1ce9c21cab12162a.jpg!q70.dpg.webp\" src=\"//wq.360buyimg.com/fd/h5/base/detail/images/transparent_a38f0a03.png\"><img item_init_src=\"//img10.360buyimg.com/imgzone/jfs/t1/52688/12/10287/42281/5d777574E4d3e2af3/629b9e1a429a4d63.jpg!q70.dpg.webp\" src=\"//wq.360buyimg.com/fd/h5/base/detail/images/transparent_a38f0a03.png\"></p><p><br></p>"
//    println(str)
//    println(str.replace(Regex("\\s+src=\"\\S+?\""), ""))

    val str2 = "<p><img style=\"width:auto;height:auto;max-width:100%;\" data-lazyload=\"http://img30.360buyimg.com/popWareDetail/jfs/t1/171030/15/3237/183474/600535b5Ee06cb851/f656618be54f41ed.jpg\" class=\"ELazy-loading\" src=\"//misc.360buyimg.com/lib/img/e/blank.gif\"><img style=\"width:auto;height:auto;max-width:100%;\" data-lazyload=\"http://img30.360buyimg.com/popWareDetail/jfs/t1/158243/28/3900/164267/600535b8E7e09a5f0/6d40261d3fcc9223.jpg\" class=\"ELazy-loading\" src=\"//misc.360buyimg.com/lib/img/e/blank.gif\"><img data-lazyload=\"//img10.360buyimg.com/imgzone/jfs/t1/52688/12/10287/42281/5d777574E4d3e2af3/629b9e1a429a4d63.jpg\" style=\"width: auto; height: auto; max-width: 100%;\" class=\"ELazy-loading\" src=\"//misc.360buyimg.com/lib/img/e/blank.gif\"><br></p><p><br></p><br>"
    println(str2)
    println(str2
        .replace(Regex("\\s+src=\"\\S+?\""), "")
        .replace("class=\"ELazy-loading\"", "")
        .replace("data-lazyload", "src")
        )
}