package com.geekq.miaosha.controller;

import com.geekq.miaosha.common.resultbean.ResultGeekQ;
import com.geekq.miaosha.domain.MiaoshaUser;
import com.geekq.miaosha.redis.GoodsKey;
import com.geekq.miaosha.redis.RedisService;
import com.geekq.miaosha.service.GoodsService;
import com.geekq.miaosha.service.MiaoShaUserService;
import com.geekq.miaosha.vo.GoodsDetailVo;
import com.geekq.miaosha.vo.GoodsVo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
@RequestMapping("/goods")
public class GoodsController extends BaseController {
    private static Logger log = LoggerFactory.getLogger(GoodsController.class);

    @Autowired
    private MiaoShaUserService userService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    ThymeleafViewResolver viewResolver;

    @Autowired
    ApplicationContext applicationContext;

    /**
     * QPS:1267 load:15 mysql
     * 5000 * 10
     * QPS:2884, load:5
     * */
    @RequestMapping(value="/to_list", produces="text/html")
    public String list(HttpServletRequest request, HttpServletResponse response, Model model, MiaoshaUser user) {
        model.addAttribute("user", user);
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        model.addAttribute("goodsList", goodsList);
        // html 放入 redis 设置时间 60秒
        return render(request,response,model,"goods_list",GoodsKey.getGoodsList,"");
    }

    @RequestMapping(value="/to_detail2/{goodsId}",produces="text/html")
    @ResponseBody
    public String detail2(HttpServletRequest request, HttpServletResponse response, Model model,MiaoshaUser user,
                          @PathVariable("goodsId")long goodsId) {
        model.addAttribute("user", user);

        //取缓存
        String html = redisService.get(GoodsKey.getGoodsDetail, ""+goodsId, String.class);
        if(!StringUtils.isEmpty(html)) {
            return html;
        }
        //手动渲染
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        model.addAttribute("goods", goods);

        long startAt = goods.getStartDate().getTime();
        long endAt = goods.getEndDate().getTime();
        long now = System.currentTimeMillis();

        int miaoshaStatus = 0;
        int remainSeconds = 0;
        if(now < startAt ) {//秒杀还没开始，倒计时
            miaoshaStatus = 0;
            remainSeconds = (int)((startAt - now )/1000);
        }else  if(now > endAt){//秒杀已经结束
            miaoshaStatus = 2;
            remainSeconds = -1;
        }else {//秒杀进行中
            miaoshaStatus = 1;
            remainSeconds = 0;
        }
        model.addAttribute("miaoshaStatus", miaoshaStatus);
        model.addAttribute("remainSeconds", remainSeconds);
//        return "goods_detail";

        IWebContext ctx = new WebContext(request,response,
                request.getServletContext(),request.getLocale(), model.asMap() );
        html = viewResolver.getTemplateEngine().process("goods_detail", ctx);
        // 为了优化访问速度，应对高并发，想把页面信息全部获取出来存到redis缓存中，这样每次访问就不用客户端进行渲染了
        if(!StringUtils.isEmpty(html)) {
            redisService.set(GoodsKey.getGoodsDetail, ""+goodsId, html);
        }
        return html;
    }

    /**
     * 数据库很少使用long的　，　id 正常使一般使用　snowflake 分布式自增id
     * @param model
     * @param user
     * @param goodsId
     * @return
     */
    @RequestMapping(value="/detail/{goodsId}")
    @ResponseBody
    public ResultGeekQ<GoodsDetailVo> detail(HttpServletRequest request, HttpServletResponse response, Model model,MiaoshaUser user,
                                        @PathVariable("goodsId")long goodsId) {
        ResultGeekQ<GoodsDetailVo> result = ResultGeekQ.build();
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        long startAt = goods.getStartDate().getTime();
        long endAt = goods.getEndDate().getTime();
        long now = System.currentTimeMillis();
        int miaoshaStatus = 0;
        int remainSeconds = 0;
        if(now < startAt ) {//秒杀还没开始，倒计时
            miaoshaStatus = 0;
            remainSeconds = (int)((startAt - now )/1000);
        }else  if(now > endAt){//秒杀已经结束
            miaoshaStatus = 2;
            remainSeconds = -1;
        }else {//秒杀进行中
            miaoshaStatus = 1;
            remainSeconds = 0;
        }
        GoodsDetailVo vo = new GoodsDetailVo();
        vo.setGoods(goods);
        vo.setUser(user);
        vo.setRemainSeconds(remainSeconds);
        vo.setMiaoshaStatus(miaoshaStatus);
        result.setData(vo);
        return result;
    }
}
