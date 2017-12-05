package com.system.controller;

import com.system.exception.CustomException;
import com.system.po.*;
import com.system.service.ComputerProblemsService;
import com.system.service.EngineRoomInspectionService;
import com.system.service.MaterialApplicationService;
import com.system.service.UserloginService;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 /**
 * 项目名称：7ThHospInfoMaintSystem
 * 类名称：NormalController
 * 类描述：普通用户请求拦截器
 * 创建人：lxk
 * 创建时间：2017-12-3 14:07:07
 * 修改人：
 * 修改时间：
 * 修改备注：
 **/

@Controller
@RequestMapping("/normal")
public class NormalController {

    @SuppressWarnings("SpringJavaAutowiringInspection")//忽略警告，下同
    @Resource(name = "userloginServiceImpl")
    private UserloginService userloginService;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Resource(name = "computerProblemsServiceImpl")
    private ComputerProblemsService computerProblemsService;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Resource(name = "materialApplicationServiceImpl")
    private MaterialApplicationService materialApplicationService;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Resource(name = "engineRoomInspectionServiceImpl")
    private EngineRoomInspectionService engineRoomInspectionService;



    /*<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<电脑故障操作>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>*/

    // 电脑故障显示
    @RequestMapping("/showComputerProblems")
    public String showComputerProblems(Model model, Integer page) throws Exception {

        List<ComputerProblemsCustom> list = null;
        //页码对象
        PagingVO pagingVO = new PagingVO();
        //设置总页数
        pagingVO.setTotalCount(computerProblemsService.getCountComputerProblems());
        if (page == null || page == 0) {
            pagingVO.setToPageNo(1);
            list = computerProblemsService.findByPaging(1);
        } else {
            pagingVO.setToPageNo(page);
            list = computerProblemsService.findByPaging(page);
        }

        model.addAttribute("computerProblemsList", list);
        model.addAttribute("pagingVO", pagingVO);

        return "normal/showComputerProblems";

    }

    //添加电脑故障
    @RequestMapping(value = "/addComputerProblems", method = {RequestMethod.GET})
    public String addComputerProblemsUI(Model model) throws Exception {

        return "normal/addComputerProblems";
    }

    // 添加电脑故障处理
    @RequestMapping(value = "/addComputerProblems", method = {RequestMethod.POST})
    public String addComputerProblemsCustom(ComputerProblemsCustom computerProblemsCustom, Model model,HttpServletRequest request,UploadedImageFile file) throws Exception {

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        Userlogin userlogin = userloginService.findByName((String) subject.getPrincipal());



        //设置问题初始化时间
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);

        //无初始时间时设置初始时间
        if(computerProblemsCustom.getCreateTime() == null || computerProblemsCustom.getCreateTime().length() == 0)
        {
            computerProblemsCustom.setCreateTime(dateString);
        }

        //文件上传至服务器并保存图片路径
        if(!file.getPhoto().isEmpty())
        {
            String name = RandomStringUtils.randomAlphanumeric(10);
            String newFileName = name + ".jpg";
            File newFile = new File(request.getServletContext().getRealPath("/upload"), newFileName);
            newFile.getParentFile().mkdirs();
            file.getPhoto().transferTo(newFile);
            //保存路径
            computerProblemsCustom.setImg(newFileName);
        }

        //设置问题初始化状态
        computerProblemsCustom.setFlag(0);

        //设置问题所属部门
        computerProblemsCustom.setDept(userlogin.getDepart());

        //设置问题所属部门编码
        computerProblemsCustom.setDepartcode(userlogin.getDepartcode());

        //设置问题所属人员ID
        computerProblemsCustom.setUserid(userlogin.getUsername());

        Boolean result = computerProblemsService.save(computerProblemsCustom);

        if (!result) {
            model.addAttribute("message", "抱歉，故障信息保存失败");
            return "error";
        }


        //重定向
        return "redirect:/normal/showComputerProblems";
    }

    // 修改电脑故障页面显示
    @RequestMapping(value = "/editComputerProblems", method = {RequestMethod.GET})
    public String editComputerProblemsUI(Integer id, Model model) throws Exception {
        if (id == null) {
            return "redirect:/normal/showComputerProblems";
        }
        ComputerProblems computerProblems = computerProblemsService.findById(id);
        if (computerProblems == null) {
            throw new CustomException("抱歉，未找到该故障相关信息");
        }

        model.addAttribute("computerProblems", computerProblems);


        return "normal/editComputerProblems";
    }

    // 修改电脑故障页面处理
    @RequestMapping(value = "/editComputerProblems", method = {RequestMethod.POST})
    public String editComputerProblems(ComputerProblemsCustom computerProblemsCustom) throws Exception {

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        Userlogin userlogin = userloginService.findByName((String) subject.getPrincipal());

        computerProblemsCustom.setLeader(userlogin.getName());

        computerProblemsService.updataById(computerProblemsCustom.getId(), computerProblemsCustom);

        //重定向
        return "redirect:/normal/showComputerProblems";
    }

    // 开始处理电脑故障
    @RequestMapping(value = "/dealComputerProblems")
    public String dealComputerProblems(HttpServletRequest request) throws Exception {

        Integer id = Integer.parseInt(request.getParameter("id"));
        String feedback = request.getParameter("feedback");


        if (id == null) {
            return "redirect:/normal/showComputerProblems";
        }

        //获取当前故障问题
        ComputerProblemsCustom computerProblemsCustom = computerProblemsService.findById(id);
        if (computerProblemsCustom == null) {
            throw new CustomException("抱歉，未找到该故障相关信息");
        }

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        Userlogin userlogin = userloginService.findByName((String) subject.getPrincipal());
        if(computerProblemsCustom.getFlag() == 0){
            //更新该故障问题数据
            computerProblemsCustom.setFlag(1);
            computerProblemsCustom.setLeader(userlogin.getName());
            computerProblemsCustom.setReback(feedback);
            computerProblemsService.updataById(computerProblemsCustom.getId(), computerProblemsCustom);
        }

       return "redirect:editComputerProblems?id=" + computerProblemsCustom.getId();
    }

    // 电脑故障处理完成
    @RequestMapping(value = "/completeComputerProblems")
    public String completeComputerProblems(HttpServletRequest request) throws Exception {

        Integer id = Integer.parseInt(request.getParameter("id"));
        String feedback = request.getParameter("feedback");


        if (id == null) {
            return "redirect:/normal/showComputerProblems";
        }

        //获取当前故障问题
        ComputerProblemsCustom computerProblemsCustom = computerProblemsService.findById(id);
        if (computerProblemsCustom == null) {
            throw new CustomException("抱歉，未找到该故障相关信息");
        }

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        Userlogin userlogin = userloginService.findByName((String) subject.getPrincipal());
        if(computerProblemsCustom.getFlag() == 0){
            //更新该故障问题数据
            computerProblemsCustom.setFlag(2);
            computerProblemsCustom.setLeader(userlogin.getName());
            computerProblemsCustom.setReback(feedback);
            computerProblemsService.updataById(computerProblemsCustom.getId(), computerProblemsCustom);
        }

        return "redirect:editComputerProblems?id=" + computerProblemsCustom.getId();
    }

    // 查看电脑故障详情
    @RequestMapping(value = "/checkComputerProblems", method = {RequestMethod.GET})
    public String checkComputerProblems(Integer id, Model model) throws Exception {
        if (id == null) {
            return "redirect:/normal/showComputerProblems";
        }
        ComputerProblems computerProblems = computerProblemsService.findById(id);
        if (computerProblems == null) {
            throw new CustomException("抱歉，未找到该故障相关信息");
        }

        model.addAttribute("computerProblems", computerProblems);


        return "normal/checkComputerProblems";
    }

    // 查看电脑故障详情
    @RequestMapping(value = "/checkComputerProblems", method = {RequestMethod.POST})
    public String checkComputerProblems(ComputerProblemsCustom computerProblemsCustom) throws Exception {

        computerProblemsService.updataById(computerProblemsCustom.getId(), computerProblemsCustom);

        //重定向
        return "redirect:/normal/showComputerProblems";
    }

    //搜索电脑故障
    @RequestMapping(value = "/searchComputerProblems")
    private String searchComputerProblems(String findByDept,String findByName,String findByFlag, Model model) throws Exception {


        List<ComputerProblemsCustom> listByDept = new ArrayList<ComputerProblemsCustom>();
        List<ComputerProblemsCustom> listByName = new ArrayList<ComputerProblemsCustom>();
        List<ComputerProblemsCustom> listByFlag = new ArrayList<ComputerProblemsCustom>();
        List<ComputerProblemsCustom> listResult = new ArrayList<ComputerProblemsCustom>();

        if(!findByDept.equals(""))
        {
            listByDept = computerProblemsService.findByDept(findByDept);
        }

        if(!findByName.equals(""))
        {
            listByName = computerProblemsService.findByName(findByName);
        }

        if(!findByFlag.equals(""))
        {
            Integer flag = Integer.parseInt(findByFlag);
            listByFlag = computerProblemsService.findByFlag(flag);
        }



        //合并去重
        listResult.addAll(listByDept);
        listResult.removeAll(listByFlag);
        listResult.addAll(listByFlag);
        listResult.removeAll(listByName);
        listResult.addAll(listByName);

        model.addAttribute("computerProblemsList", listResult);
        return "normal/showComputerProblems";
    }

    /*<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<物资申购>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>*/
    // 物资申购显示
    @RequestMapping("/showMaterialApplication")
    public String showMaterialApplication(Model model, Integer page) throws Exception {
        List<MaterialApplicationCustom> list = null;
        //页码对象
        PagingVO pagingVO = new PagingVO();
        //设置总页数
        pagingVO.setTotalCount(materialApplicationService.getCountMaterialApplication());
        if (page == null || page == 0) {
            pagingVO.setToPageNo(1);
            list = materialApplicationService.findByPaging(1);
        } else {
            pagingVO.setToPageNo(page);
            list = materialApplicationService.findByPaging(page);
        }

        model.addAttribute("materialApplicationList", list);
        model.addAttribute("pagingVO", pagingVO);

        return "normal/showMaterialApplication";

    }

    //添加物资申购
    @RequestMapping(value = "/addMaterialApplication", method = {RequestMethod.GET})
    public String addMaterialApplicationUI(Model model) throws Exception {

        return "normal/addMaterialApplication";
    }

    // 添加物资申购
    @RequestMapping(value = "/addMaterialApplication", method = {RequestMethod.POST})
    public String addMaterialApplicationCustom(MaterialApplicationCustom materialApplicationCustom, Model model) throws Exception {

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        Userlogin userlogin = userloginService.findByName((String) subject.getPrincipal());



        //设置问题初始化时间
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);

        //无初始时间时设置初始时间
        if(materialApplicationCustom.getCreateTime() == null || materialApplicationCustom.getCreateTime().length() == 0)
        {
            materialApplicationCustom.setCreateTime(dateString);
        }

        //设置问题初始化状态
        materialApplicationCustom.setFlag(0);

        //设置问题所属部门
        materialApplicationCustom.setDept(userlogin.getDepart());

        //设置问题所属部门编码
        materialApplicationCustom.setDepartcode(userlogin.getDepartcode());

        //设置问题所属人员ID
        materialApplicationCustom.setUserid(userlogin.getUsername());

        Boolean result = materialApplicationService.save(materialApplicationCustom);

        if (!result) {
            model.addAttribute("message", "抱歉，物资申购信息保存失败");
            return "error";
        }


        //重定向
        return "redirect:/normal/showMaterialApplication";
    }

    // 修改物资申购页面显示
    @RequestMapping(value = "/editMaterialApplication", method = {RequestMethod.GET})
    public String editMaterialApplicationUI(Integer id, Model model) throws Exception {
        if (id == null) {
            return "redirect:/normal/showMaterialApplication";
        }
        MaterialApplication materialApplication = materialApplicationService.findById(id);
        if (materialApplication == null) {
            throw new CustomException("抱歉，未找到该物资申购相关信息");
        }

        model.addAttribute("materialApplication", materialApplication);


        return "normal/editMaterialApplication";
    }

    // 修改物资申购页面处理
    @RequestMapping(value = "/editMaterialApplication", method = {RequestMethod.POST})
    public String editMaterialApplication(MaterialApplicationCustom materialApplicationCustom) throws Exception {

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        Userlogin userlogin = userloginService.findByName((String) subject.getPrincipal());

        materialApplicationCustom.setLeader(userlogin.getName());

        materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);

        //重定向
        return "redirect:/normal/showMaterialApplication";
    }

    // 开始处理物资申购
    @RequestMapping(value = "/dealMaterialApplication")
    public String dealMaterialApplication(HttpServletRequest request) throws Exception {

        Integer id = Integer.parseInt(request.getParameter("id"));
        String feedback = request.getParameter("feedback");


        if (id == null) {
            return "redirect:/normal/showMaterialApplication";
        }

        //获取当前物资申购问题
        MaterialApplicationCustom materialApplicationCustom = materialApplicationService.findById(id);
        if (materialApplicationCustom == null) {
            throw new CustomException("抱歉，未找到该物资申购相关信息");
        }

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        Userlogin userlogin = userloginService.findByName((String) subject.getPrincipal());
        if(materialApplicationCustom.getFlag() == 0){
            //更新该物资申购问题数据
            materialApplicationCustom.setFlag(1);
            materialApplicationCustom.setLeader(userlogin.getName());
            materialApplicationCustom.setReback(feedback);
            materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
        }

        return "redirect:editMaterialApplication?id=" + materialApplicationCustom.getId();
    }

    // 物资申购处理完成
    @RequestMapping(value = "/completeMaterialApplication")
    public String completeMaterialApplication(HttpServletRequest request) throws Exception {

        Integer id = Integer.parseInt(request.getParameter("id"));
        String feedback = request.getParameter("feedback");


        if (id == null) {
            return "redirect:/normal/showMaterialApplication";
        }

        //获取当前物资申购信息
        MaterialApplicationCustom materialApplicationCustom = materialApplicationService.findById(id);
        if (materialApplicationCustom == null) {
            throw new CustomException("抱歉，未找到该物资申购相关信息");
        }

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        Userlogin userlogin = userloginService.findByName((String) subject.getPrincipal());
        if(materialApplicationCustom.getFlag() == 1){
            //更新该物资申购问题数据
            materialApplicationCustom.setFlag(2);
            materialApplicationCustom.setLeader(userlogin.getName());
            materialApplicationCustom.setReback(feedback);
            materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
        }

        return "redirect:editMaterialApplication?id=" + materialApplicationCustom.getId();
    }

    // 查看物资申购详情
    @RequestMapping(value = "/checkMaterialApplication", method = {RequestMethod.GET})
    public String checkMaterialApplication(Integer id, Model model) throws Exception {
        if (id == null) {
            return "redirect:/normal/showMaterialApplication";
        }
        MaterialApplication materialApplication = materialApplicationService.findById(id);
        if (materialApplication == null) {
            throw new CustomException("抱歉，未找到该物资申购相关信息");
        }

        model.addAttribute("materialApplication", materialApplication);


        return "normal/checkMaterialApplication";
    }

    // 查看物资申购详情
    @RequestMapping(value = "/checkMaterialApplication", method = {RequestMethod.POST})
    public String checkMaterialApplication(MaterialApplicationCustom materialApplicationCustom) throws Exception {

        materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);

        //重定向
        return "redirect:/normal/showMaterialApplication";
    }

    //搜索物资申购
    @RequestMapping(value = "/searchMaterialApplication")
    private String searchMaterialApplication(String findByDept,String findByName,String findByFlag, Model model) throws Exception {


        List<MaterialApplicationCustom> listByDept = new ArrayList<MaterialApplicationCustom>();
        List<MaterialApplicationCustom> listByName = new ArrayList<MaterialApplicationCustom>();
        List<MaterialApplicationCustom> listByFlag = new ArrayList<MaterialApplicationCustom>();
        List<MaterialApplicationCustom> listResult = new ArrayList<MaterialApplicationCustom>();

        if(!findByDept.equals(""))
        {
            listByDept = materialApplicationService.findByDept(findByDept);
        }

        if(!findByName.equals(""))
        {
            listByName = materialApplicationService.findByName(findByName);
        }

        if(!findByFlag.equals(""))
        {
            Integer flag = Integer.parseInt(findByFlag);
            listByFlag = materialApplicationService.findByFlag(flag);
        }



        //合并去重
        listResult.addAll(listByDept);
        listResult.removeAll(listByFlag);
        listResult.addAll(listByFlag);
        listResult.removeAll(listByName);
        listResult.addAll(listByName);

        model.addAttribute("materialApplicationList", listResult);
        return "normal/showMaterialApplication";
    }

    /*<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<机房巡检>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>*/
    // 机房巡检显示
    @RequestMapping("/showEngineRoomInspection")
    public String showEngineRoomInspection(Model model, Integer page) throws Exception {
        List<EngineRoomInspectionCustom> list = null;
        //页码对象
        PagingVO pagingVO = new PagingVO();
        //设置总页数
        pagingVO.setTotalCount(engineRoomInspectionService.getCountEngineRoomInspection());
        if (page == null || page == 0) {
            pagingVO.setToPageNo(1);
            list = engineRoomInspectionService.findByPaging(1);
        } else {
            pagingVO.setToPageNo(page);
            list = engineRoomInspectionService.findByPaging(page);
        }

        model.addAttribute("engineRoomInspectionList", list);
        model.addAttribute("pagingVO", pagingVO);

        return "normal/showEngineRoomInspection";

    }

    //添加机房巡检
    @RequestMapping(value = "/addEngineRoomInspection", method = {RequestMethod.GET})
    public String addEngineRoomInspectionUI(Model model) throws Exception {

        return "normal/addEngineRoomInspection";
    }

    // 添加机房巡检
    @RequestMapping(value = "/addEngineRoomInspection", method = {RequestMethod.POST})
    public String addEngineRoomInspectionCustom(EngineRoomInspectionCustom engineRoomInspectionCustom, Model model) throws Exception {

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        Userlogin userlogin = userloginService.findByName((String) subject.getPrincipal());



        //设置问题初始化时间
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);

        //无初始时间时设置初始时间
        if(engineRoomInspectionCustom.getCreateTime() == null || engineRoomInspectionCustom.getCreateTime().length() == 0)
        {
            engineRoomInspectionCustom.setCreateTime(dateString);
        }

        //设置问题所属人员ID
        engineRoomInspectionCustom.setUserid(userlogin.getUsername());

        Boolean result = engineRoomInspectionService.save(engineRoomInspectionCustom);

        if (!result) {
            model.addAttribute("message", "抱歉，机房巡检信息保存失败");
            return "error";
        }


        //重定向
        return "redirect:/normal/showEngineRoomInspection";
    }

/*    // 修改机房巡检页面显示
    @RequestMapping(value = "/editEngineRoomInspection", method = {RequestMethod.GET})
    public String editEngineRoomInspectionUI(Integer id, Model model) throws Exception {
        if (id == null) {
            return "redirect:/normal/showEngineRoomInspection";
        }
        EngineRoomInspection engineRoomInspection = engineRoomInspectionService.findById(id);
        if (engineRoomInspection == null) {
            throw new CustomException("抱歉，未找到该机房巡检相关信息");
        }

        model.addAttribute("engineRoomInspection", engineRoomInspection);


        return "normal/editEngineRoomInspection";
    }*/

    // 修改机房巡检页面处理
/*    @RequestMapping(value = "/editEngineRoomInspection", method = {RequestMethod.POST})
    public String editEngineRoomInspection(EngineRoomInspectionCustom engineRoomInspectionCustom) throws Exception {

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        Userlogin userlogin = userloginService.findByName((String) subject.getPrincipal());

        engineRoomInspectionService.updataById(engineRoomInspectionCustom.getId(), engineRoomInspectionCustom);

        //重定向
        return "redirect:/normal/showEngineRoomInspection";
    }*/

/*    // 开始处理机房巡检
    @RequestMapping(value = "/dealEngineRoomInspection")
    public String dealEngineRoomInspection(HttpServletRequest request) throws Exception {

        Integer id = Integer.parseInt(request.getParameter("id"));
        String feedback = request.getParameter("ycyy");


        if (id == null) {
            return "redirect:/normal/showEngineRoomInspection";
        }

        //获取当前机房巡检问题
        EngineRoomInspectionCustom engineRoomInspectionCustom = engineRoomInspectionService.findById(id);
        if (engineRoomInspectionCustom == null) {
            throw new CustomException("抱歉，未找到该机房巡检相关信息");
        }

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        Userlogin userlogin = userloginService.findByName((String) subject.getPrincipal());
        engineRoomInspectionCustom.setYcyy(feedback);
        engineRoomInspectionService.updataById(engineRoomInspectionCustom.getId(), engineRoomInspectionCustom);

        return "redirect:editEngineRoomInspection?id=" + engineRoomInspectionCustom.getId();
    }*/

/*    // 机房巡检处理完成
    @RequestMapping(value = "/completeEngineRoomInspection")
    public String completeEngineRoomInspection(HttpServletRequest request) throws Exception {

        Integer id = Integer.parseInt(request.getParameter("id"));
        String feedback = request.getParameter("feedback");


        if (id == null) {
            return "redirect:/normal/showEngineRoomInspection";
        }

        //获取当前机房巡检信息
        EngineRoomInspectionCustom engineRoomInspectionCustom = engineRoomInspectionService.findById(id);
        if (engineRoomInspectionCustom == null) {
            throw new CustomException("抱歉，未找到该机房巡检相关信息");
        }

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        Userlogin userlogin = userloginService.findByName((String) subject.getPrincipal());
        engineRoomInspectionCustom.setYcyy(feedback);
        engineRoomInspectionService.updataById(engineRoomInspectionCustom.getId(), engineRoomInspectionCustom);


        return "redirect:editEngineRoomInspection?id=" + engineRoomInspectionCustom.getId();
    }*/

    // 查看机房巡检详情
    @RequestMapping(value = "/checkEngineRoomInspection", method = {RequestMethod.GET})
    public String checkEngineRoomInspection(Integer id, Model model) throws Exception {
        if (id == null) {
            return "redirect:/normal/showEngineRoomInspection";
        }
        EngineRoomInspection engineRoomInspection = engineRoomInspectionService.findById(id);
        if (engineRoomInspection == null) {
            throw new CustomException("抱歉，未找到该机房巡检相关信息");
        }

        model.addAttribute("engineRoomInspection", engineRoomInspection);


        return "normal/checkEngineRoomInspection";
    }

    // 查看机房巡检详情
    @RequestMapping(value = "/checkEngineRoomInspection", method = {RequestMethod.POST})
    public String checkEngineRoomInspection(EngineRoomInspectionCustom engineRoomInspectionCustom) throws Exception {

        engineRoomInspectionService.updataById(engineRoomInspectionCustom.getId(), engineRoomInspectionCustom);

        //重定向
        return "redirect:/normal/showEngineRoomInspection";
    }

    //搜索机房巡检
    @RequestMapping(value = "/searchEngineRoomInspection")
    private String searchEngineRoomInspection(String findByExaminer, Model model) throws Exception {


        List<EngineRoomInspectionCustom> listByExaminer = new ArrayList<EngineRoomInspectionCustom>();
        List<EngineRoomInspectionCustom> listResult = new ArrayList<EngineRoomInspectionCustom>();

        if(!findByExaminer.equals(""))
        {
            listByExaminer = engineRoomInspectionService.findByExaminer(findByExaminer);
        }

        listResult.addAll(listByExaminer);

        model.addAttribute("engineRoomInspectionList", listResult);
        return "normal/showEngineRoomInspection";
    }

}