/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-03-19 16:56
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

import com.github.rwsbillyang.spider.SeleniumSpider
import com.github.rwsbillyang.spider.Spider
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.interactions.Actions

import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeUnit

@Serializable
class TaobaoProduct(
    val id: String?, //商品Id  https://detail.tmall.com/item.htm?id=13951260622
    val name: String?, //商品名称
    val img: String?, // 主图
    val price: String?, //价格
    val buyers: String?, //购买者数量
    val vendor: String?, //店铺名称
    val from: String?, //地址
    val number: Int? = null //综合排序，从1开始
)


class TaobaoSpider(private val username: String, private val password: String, binary: String? = null,uas: Array<String> = Spider.UAs_PC) : SeleniumSpider(binary, uas)  {
    private val log: Logger = LoggerFactory.getLogger("TaobaoSpider")

    private val mobileLoginUrl = "https://login.m.taobao.com/login.htm"

    var isLogin = false


    override fun doParse(url: String): Map<String, String?> {
        val driver: WebDriver = ChromeDriver(chromeOptions)

        if(!isLogin){
            userMobileLogin(driver)
        }

        Thread.sleep(2000)


        val map = mutableMapOf<String, String?>()

        try {
            log.info("parse url=$url")
            driver.get(url)// 目标地址
            val body: WebElement =  WebDriverWait(driver, Duration.ofSeconds(15))
                .until { d -> driver.findElement(By.tagName("body")) }
            log.info(body.text)

            //注意：class必须严格字符串匹配，包括空格
            val itemList = WebDriverWait(driver, Duration.ofSeconds(15))
                .until { d -> driver.findElements(By.xpath("//div[@class='item J_MouserOnverReq  ']")) }

            var number = 1
            val list = itemList.map {
                parsePcItem(it, number++)
            }

            val listStr = Json.encodeToString(list)

            log.info("list=$listStr")

            map[Spider.CONTENT] = listStr
            map[Spider.MSG] = "恭喜，解析成功"
            map[Spider.RET] = Spider.OK
        } catch (e: IOException) {
            log.error("IOException: ${e.message},url=$url")
            map[Spider.MSG] = "获取内容时IO错误，请稍后再试"
            map[Spider.RET] = Spider.KO
        }catch (e: Exception){
            e.printStackTrace()
            log.error("Exception: ${e.message}, url=$url")
            map[Spider.MSG] = "获取内容时出现错误，请稍后再试"
            map[Spider.RET] = Spider.KO
        }
        finally {
            driver.close()
        }


        return map
    }


    /**
     * 登录模块
     */
    @Throws(java.lang.Exception::class)
    private fun userMobileLogin(driver: WebDriver) {
        log.info("userMobileLogin...")
        driver[mobileLoginUrl]

        Thread.sleep(200) //等待0.2秒
        val usernameWebElement = driver.findElement(By.id("fm-login-id"))
        usernameWebElement.sendKeys(username)
        val passwordWebElement = driver.findElement(By.id("fm-login-password"))
        passwordWebElement.sendKeys(password)

        Thread.sleep((800..1500).random().toLong())
        //模拟滑动
        val draggable = driver.findElement(By.id("nc_1_n1z")) //定位元素
        if(draggable != null){
            val bu = Actions(driver) // 声明action对象
            bu.clickAndHold(draggable).build().perform() // clickAndHold鼠标左键按下draggable元素不放
            bu.moveByOffset(380, 2).perform() // 平行移动鼠标
            Thread.sleep((300..500).random().toLong())
            bu.moveByOffset(400, 2).perform() // 平行移动鼠标
            Thread.sleep((300..500).random().toLong())
            bu.moveByOffset(420, 2).perform() // 平行移动鼠标
            Thread.sleep((1000..2000).random().toLong())
        }

        val btnWebElement = driver.findElement(By.xpath("//button[@class='fm-button fm-submit password-login']"))
        btnWebElement.click()

        isLogin = true

        log.info("userMobileLogin Done")
    }





    private fun parsePcItem(element: WebElement, number: Int): TaobaoProduct{
        //log.info("parsePcItem="+element.text)

        val imgElement = element.findElement(By.tagName("img"))
        val img = imgElement.getAttribute("src")?:imgElement.getAttribute("data-src")// 主图
        val id = imgElement.getAttribute("id")?.substringAfter("J_Itemlist_Pic_") //商品Id
        val name = imgElement.getAttribute("alt") //商品名称

        val price = element.findElement(By.className("price")).findElement(By.tagName("strong")).text //价格
        val buyes = element.findElement(By.className("deal-cnt"))?.text?.substringBefore("人") //购买者数量  331人付款 6500+人收货
        val vendor = element.findElement(By.className("shop")).text//店铺名称
        val from = element.findElement(By.className("location")).text //地址

        return TaobaoProduct(id, name, img, price, buyes, vendor, from, number)
    }

    private fun parseByJsoup(element: WebElement): TaobaoProduct{
        val doc = Jsoup.parse(element.getAttribute("outerHTML"))
        val imgElement = doc.select("img")
        val img = imgElement.attr("src")?:imgElement.attr("data-src") // 主图
        val id = imgElement.attr("id")?.substringAfter("J_Itemlist_Pic_") //商品Id
        val name = imgElement.attr("alt") //商品名称

        val price = doc.select("div.price > strong").text() //价格
        val buyes = doc.select("div.deal-cnt").text() //购买者数量
        val vendor = doc.select("div.shop").text() //店铺名称
        val from = doc.select("div.location").text()  //地址

        return TaobaoProduct(id, name, img, price, buyes, vendor, from)
    }


    private fun parseMobileItem(element: WebElement){

       // val id = titleElement.findElement(By.tagName("a")).getAttribute("data-nid") //商品Id
        val name = element.findElement(By.className("d-title")).text //商品名称
        val img = element.findElement(By.tagName("img")).getAttribute("src") // 主图
        val price = element.findElement(By.className("d-price")).findElement(By.tagName("em")).findElement(By.className("font-num")).text //价格

        val dMain = element.findElement(By.className("d-main"))
        val buyes = dMain.findElement(By.className("font-num")).text //购买者数量
        val from = dMain.findElement(By.className("d-area")).text //地址

        //val vendor = element.findElement(By.className("shop")).text//店铺名称
       // TaobaoProduct(id, name, img, price, buyes, vendor, from)
    }
}

//https://s.m.taobao.com/h5?&_input_charset=utf-8&q=%E7%A1%92%E7%89%87
//https://s.taobao.com/search?q=%E7%A1%92%E7%89%87
//https://s.taobao.com/search?q=%E7%A1%92%E7%89%87&sort=sale-desc
fun main(args: Array<String>) {
    TaobaoSpider("your_taobao_username", "your_taobao_pwd",  "/Users/bill/git/youke/server/app/mainApp/chromedriver")
        .doParse("https://s.taobao.com/search?q=%E7%A1%92%E7%89%87&sort=sale-desc")
        .forEach {
            println("${it.key}=${it.value}")
        }
}

