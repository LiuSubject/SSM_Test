package com.system.controller;

import com.system.exception.CustomException;
import com.system.po.*;
import com.system.util.push.CreatePushUtil;
import com.system.service.*;
import com.system.util.CustomerContextHolder;
import com.system.util.push.MessagePushUtil;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 项目名称：7ThHospInfoMaintSystem
 * 类名称：AdminController
 * 类描述：PC端管理员请求拦截器
 * 创建人：lxk
 * 创建时间：2017-12-3 14:01:53
 * 修改人：
 * 修改时间：
 * 修改备注：
 **/

@Controller
@RequestMapping("/admin")
public class AdminController {


    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Resource(name = "computerProblemsServiceImpl")
    private ComputerProblemsService computerProblemsService;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Resource(name = "materialApplicationServiceImpl")
    private MaterialApplicationService materialApplicationService;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Resource(name = "engineRoomInspectionServiceImpl")
    private EngineRoomInspectionService engineRoomInspectionService;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Resource(name = "roleServiceImpl")
    private RoleService roleService;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Resource(name = "viewEmployeeMiPsdServiceImpl")
    private ViewEmployeeMiPsdService viewEmployeeMiPsdService;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Resource(name = "computerProblemsTypeServiceImpl")
    private ComputerProblemsTypeService computerProblemsTypeService;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Resource(name = "materialApplicationTypeServiceImpl")
    private MaterialApplicationTypeService materialApplicationTypeService;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Resource(name = "pushMessageServiceImpl")
    private PushMessageService pushMessageService;

    @Autowired
    private CreatePushUtil createPushUtil;

    @Autowired
    private MessagePushUtil messagePushUtil;

    /*<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<电脑故障操作>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>*/

    // 电脑故障显示
    @RequestMapping("/showComputerProblems")
    public String showComputerProblems(Model model, Integer page) throws Exception {

        Subject subject = SecurityUtils.getSubject();
        int groupType = 0;
        if (subject.hasRole("hardware")) {
            groupType = 1;
        }else if(subject.hasRole("software")){
            groupType = 2;
        }else if(subject.hasRole("fee")){
            groupType = 3;
        }else{

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

            return "admin/showComputerProblems";

        }
        List<ComputerProblemsCustom> list = null;
        //页码对象
        PagingVO pagingVO = new PagingVO();
        //设置总页数
        pagingVO.setTotalCount(computerProblemsService.getCountGroupComputerProblems(groupType));
        if (page == null || page == 0) {
            pagingVO.setToPageNo(1);
            list = computerProblemsService.findGroupByPaging(1, groupType);
        } else {
            pagingVO.setToPageNo(page);
            list = computerProblemsService.findGroupByPaging(page, groupType);
        }

        model.addAttribute("computerProblemsList", list);
        model.addAttribute("pagingVO", pagingVO);

        return "admin/showComputerProblems";

    }

    //添加电脑故障
    @RequestMapping(value = "/addComputerProblems", method = {RequestMethod.GET})
    public String addComputerProblemsUI(Model model) throws Exception {

        return "admin/addComputerProblems";
    }

    // 添加电脑故障处理
    @RequestMapping(value = "/addComputerProblems", method = {RequestMethod.POST})
    @Transactional
    public String addComputerProblemsCustom(ComputerProblemsCustom computerProblemsCustom, Model model,HttpServletRequest request,UploadedImageFile file) throws Exception {

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        ViewEmployeeMiPsd viewEmployeeMiPsd = null;
        try {
            //切换数据源至SQLServer
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MSSQL);
            viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());
            //切换数据源至MySQL
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
        } catch (Exception e) {
            //切换数据源至MySQL(启用备用库)
            try{
                CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
                viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());

            }catch (Exception eSwitch){
                eSwitch.printStackTrace();
            }
            e.printStackTrace();
        }



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

        //设置问题所属部门编码
        computerProblemsCustom.setDepartcode(viewEmployeeMiPsd.getDeptCode());

        //设置问题所属人员ID
        computerProblemsCustom.setUserid(viewEmployeeMiPsd.getCode());

        //设置标红标识
        computerProblemsCustom.setFaultUrgent(0);

        //保存该记录相关数据以便产生推送
        try {
            //创建消息
            PushMessage preMessage = createPushUtil.CreatePreMessage(computerProblemsCustom.getUserid(),"0","0",
                    "3","11");
            Boolean result = computerProblemsService.saveAndPre(computerProblemsCustom, preMessage);
            if (!result) {
                model.addAttribute("message", "抱歉，故障信息保存失败");
                return "error";
            }
            //向指定管理组推送消息
            switch(computerProblemsCustom.getType()){
                case 1:
                    String[] userGroups1 = new String[]{"hardware","examiner"};
                    messagePushUtil.GroupsPushSingle(preMessage,userGroups1);
                    break;
                case 2:
                    String[] userGroups2 = new String[]{"software","examiner"};
                    messagePushUtil.GroupsPushSingle(preMessage,userGroups2);
                    break;
                case 3:
                    String[] userGroups3 = new String[]{"fee","examiner"};
                    messagePushUtil.GroupsPushSingle(preMessage,userGroups3);
                    break;
                default:
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "error";

        }
        //重定向
        return "redirect:/admin/showComputerProblems";
    }

    // 修改电脑故障页面显示
    @RequestMapping(value = "/editComputerProblems", method = {RequestMethod.GET})
    public String editComputerProblemsUI(Integer id, Model model) throws Exception {
        if (id == null) {
            return "redirect:/admin/showComputerProblems";
        }
        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        if(subject.hasRole("examiner")){
            model.addAttribute("examiner", true);
        }else{
            model.addAttribute("examiner", false);
        }
        ComputerProblems computerProblems = computerProblemsService.findById(id);
        if (computerProblems == null) {
            throw new CustomException("抱歉，未找到该故障相关信息");
        }

        model.addAttribute("computerProblems", computerProblems);


        return "admin/editComputerProblems";
    }

    // 修改电脑故障页面处理
    @RequestMapping(value = "/editComputerProblems", method = {RequestMethod.POST})
    public String editComputerProblems(ComputerProblemsCustom computerProblemsCustom) throws Exception {

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        ViewEmployeeMiPsd viewEmployeeMiPsd = null;
        try {
            //切换数据源至SQLServer
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MSSQL);
            viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());
            //切换数据源至MySQL
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
        } catch (Exception e) {
            //切换数据源至MySQL(启用备用库)
            try{
                CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
                viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());

            }catch (Exception eSwitch){
                eSwitch.printStackTrace();
            }
            e.printStackTrace();
        }

        computerProblemsCustom.setLeader(viewEmployeeMiPsd.getCode());

        computerProblemsService.updataById(computerProblemsCustom.getId(), computerProblemsCustom);

        //重定向
        return "redirect:/admin/showComputerProblems";
    }

    // 标红指定电脑故障
    @RequestMapping(value = "/urgentComputerProblems", method = {RequestMethod.GET})
    public String urgentComputerProblems(Integer id, Model model) throws Exception {
        if (id == null) {
            return "redirect:/admin/showComputerProblems";
        }
        ComputerProblemsCustom computerProblemsCustom = computerProblemsService.findById(id);
        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        //检查权限
        if(subject.hasRole("examiner")){

            if (computerProblemsCustom == null) {
                model.addAttribute("message", "没有相关记录");
                return "error";
            }else{
                if(computerProblemsCustom.getFaultUrgent() == null || computerProblemsCustom.getFaultUrgent() == 0){
                    computerProblemsCustom.setFaultUrgent(1);
                    computerProblemsService.updataById(computerProblemsCustom.getId(), computerProblemsCustom);
                }
                //保存该记录相关数据以便产生推送
                try {

                    //产生推送消息
                    PushMessage preMessage = createPushUtil.CreatePreMessage(computerProblemsCustom.getUserid(),"0","0",
                            "0","99");
                    try {
                        pushMessageService.save(preMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "error";
                    }
                    //向指定管理组推送消息
                    switch(computerProblemsCustom.getType()){
                        case 1:
                            messagePushUtil.GroupPushSingle(preMessage,"hardware");
                            break;
                        case 2:
                            messagePushUtil.GroupPushSingle(preMessage,"software");
                            break;
                        case 3:
                            messagePushUtil.GroupPushSingle(preMessage,"fee");
                            break;
                        default:
                            break;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    return "error";

                }
            }

        }

        model.addAttribute("computerProblems", computerProblemsCustom);


        return "admin/editComputerProblems";
    }


    // 开始处理电脑故障
    @RequestMapping(value = "/dealComputerProblems")
    public String dealComputerProblems(HttpServletRequest request) throws Exception {

        Integer id = Integer.parseInt(request.getParameter("id"));
        String feedback = request.getParameter("feedback");


        if (id == null) {
            return "redirect:/admin/showComputerProblems";
        }

        //获取当前故障问题
        ComputerProblemsCustom computerProblemsCustom = computerProblemsService.findById(id);
        if (computerProblemsCustom == null) {
            throw new CustomException("抱歉，未找到该故障相关信息");
        }

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        ViewEmployeeMiPsd viewEmployeeMiPsd = null;
        try {
            //切换数据源至SQLServer
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MSSQL);
            viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());
            //切换数据源至MySQL
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
        } catch (Exception e) {
            //切换数据源至MySQL(启用备用库)
            try{
                CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
                viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());

            }catch (Exception eSwitch){
                eSwitch.printStackTrace();
            }
            e.printStackTrace();
        }
        if(computerProblemsCustom.getFlag() == 0 || computerProblemsCustom.getFlag() == 1){
            //更新该故障问题数据
            computerProblemsCustom.setFlag(1);
            //将反馈插入空白反馈字段
            if(computerProblemsCustom.getFeedbackId1() == null){
                computerProblemsCustom.setFeedbackContent1(feedback);
                computerProblemsCustom.setFeedbackId1(viewEmployeeMiPsd.getCode());
                computerProblemsCustom.setFeedbackName1(viewEmployeeMiPsd.getName());
            }else if(computerProblemsCustom.getFeedbackId2() == null){
                computerProblemsCustom.setFeedbackContent2(feedback);
                computerProblemsCustom.setFeedbackId2(viewEmployeeMiPsd.getCode());
                computerProblemsCustom.setFeedbackName2(viewEmployeeMiPsd.getName());
            }else if(computerProblemsCustom.getFeedbackId3() == null){
                computerProblemsCustom.setFeedbackContent3(feedback);
                computerProblemsCustom.setFeedbackId3(viewEmployeeMiPsd.getCode());
                computerProblemsCustom.setFeedbackName3(viewEmployeeMiPsd.getName());
            }else if(computerProblemsCustom.getFeedbackId4() == null){
                computerProblemsCustom.setFeedbackContent4(feedback);
                computerProblemsCustom.setFeedbackId4(viewEmployeeMiPsd.getCode());
                computerProblemsCustom.setFeedbackName4(viewEmployeeMiPsd.getName());
            }else if(computerProblemsCustom.getFeedbackId5() == null){
                computerProblemsCustom.setFeedbackContent5(feedback);
                computerProblemsCustom.setFeedbackId5(viewEmployeeMiPsd.getCode());
                computerProblemsCustom.setFeedbackName5(viewEmployeeMiPsd.getName());
            }
            //更新故障对象
            computerProblemsService.updataById(computerProblemsCustom.getId(), computerProblemsCustom);
            //保存该记录相关数据以便产生推送
            try {
                //创建推送消息
                PushMessage pushMessage = createPushUtil.CreatePreMessage(computerProblemsCustom.getUserid(),"0","0",
                        "2","12");
                try {
                    pushMessageService.save(pushMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                    return "error";
                }
                //向申报人推送消息
                messagePushUtil.SpecifiedPushSingle(pushMessage,computerProblemsCustom.getUserid());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        return "redirect:editComputerProblems?id=" + computerProblemsCustom.getId();
    }

    // 电脑故障处理完成
    @RequestMapping(value = "/completeComputerProblems")
    public String completeComputerProblems(HttpServletRequest request) throws Exception {

        Integer id = Integer.parseInt(request.getParameter("id"));
        String feedback = request.getParameter("feedback");


        if (id == null) {
            return "redirect:/admin/showComputerProblems";
        }

        //获取当前故障问题
        ComputerProblemsCustom computerProblemsCustom = computerProblemsService.findById(id);
        if (computerProblemsCustom == null) {
            throw new CustomException("抱歉，未找到该故障相关信息");
        }

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        ViewEmployeeMiPsd viewEmployeeMiPsd = null;
        try {
            //切换数据源至SQLServer
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MSSQL);
            viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());
            //切换数据源至MySQL
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
        } catch (Exception e) {
            //切换数据源至MySQL(启用备用库)
            try{
                CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
                viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());

            }catch (Exception eSwitch){
                eSwitch.printStackTrace();
            }
            e.printStackTrace();
        }
        if(computerProblemsCustom.getFlag() == 1){
            //更新该故障问题数据
            computerProblemsCustom.setFlag(2);
            computerProblemsCustom.setFaultUrgent(0);
            computerProblemsCustom.setLeader(viewEmployeeMiPsd. getCode());
            computerProblemsCustom.setReback(feedback);
            computerProblemsService.updataById(computerProblemsCustom.getId(), computerProblemsCustom);

            //保存该记录相关数据以便产生推送
            try {
                //创建推送消息
                PushMessage pushMessage = createPushUtil.CreatePreMessage(computerProblemsCustom.getUserid(),"0","0",
                        "2","13");
                try {
                    pushMessageService.save(pushMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                    return "error";
                }
                //向申报人推送消息
                messagePushUtil.SpecifiedPushSingle(pushMessage,computerProblemsCustom.getUserid());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        return "redirect:editComputerProblems?id=" + computerProblemsCustom.getId();
    }

    // 查看电脑故障详情
    @RequestMapping(value = "/checkComputerProblems", method = {RequestMethod.GET})
    public String checkComputerProblems(Integer id, Model model) throws Exception {
        if (id == null) {
            return "redirect:/admin/showComputerProblems";
        }
        ComputerProblems computerProblems = computerProblemsService.findById(id);
        if (computerProblems == null) {
            throw new CustomException("抱歉，未找到该故障相关信息");
        }

        model.addAttribute("computerProblems", computerProblems);


        return "admin/checkComputerProblems";
    }

    // 查看电脑故障详情
    @RequestMapping(value = "/checkComputerProblems", method = {RequestMethod.POST})
    public String checkComputerProblems(ComputerProblemsCustom computerProblemsCustom) throws Exception {

        computerProblemsService.updataById(computerProblemsCustom.getId(), computerProblemsCustom);

        //重定向
        return "redirect:/admin/showComputerProblems";
    }

    //搜索电脑故障
    @RequestMapping(value = "/searchComputerProblems")
    private String searchComputerProblems(String findByDept,String findByName,String findByFlag, Model model,Integer page) throws Exception {

        List<ComputerProblemsCustom> list = null;
        Map<String, Object> map =new HashMap<String, Object>();
        map.put("dept",findByDept);
        map.put("name",findByName);
        int flag = 0;
        try {
            flag = Integer.parseInt(findByFlag);
        } catch (NumberFormatException e) {
            flag = 3;
        }
        map.put("flag",flag);
        try {
            Subject subject = SecurityUtils.getSubject();
            int groupType = 0;
            if (subject.hasRole("hardware")) {
                groupType = 1;
                map.put("groupType",groupType);
                list = computerProblemsService.paginationOfgGroupSearchResults(map);
            }else if(subject.hasRole("software")){
                groupType = 2;
                map.put("groupType",groupType);
                list = computerProblemsService.paginationOfgGroupSearchResults(map);
            }else if(subject.hasRole("fee")){
                groupType = 3;
                map.put("groupType",groupType);
                list = computerProblemsService.paginationOfgGroupSearchResults(map);
            }else {
                list = computerProblemsService.paginationOfSearchResults(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }

        model.addAttribute("computerProblemsList", list);

        return "admin/showComputerProblems";
    }

    /*<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<物资申购>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>*/
    // 物资申购显示
    @RequestMapping("/showMaterialApplication")
    public String showMaterialApplication(Model model, Integer page) throws Exception {
        Subject subject = SecurityUtils.getSubject();

        List<MaterialApplicationCustom> list = null;
        //页码对象
        PagingVO pagingVO = new PagingVO();
        //物资组处理可处理列表
        if (subject.hasRole("material")) {
            //设置总页数
            pagingVO.setTotalCount(materialApplicationService.getCountApprovedMaterialApplication());
            if (page == null || page == 0) {
                pagingVO.setToPageNo(1);
                list = materialApplicationService.findByApprovedPaging(1);
            } else {
                pagingVO.setToPageNo(page);
                list = materialApplicationService.findByApprovedPaging(page);
            }

            model.addAttribute("materialApplicationList", list);
            model.addAttribute("pagingVO", pagingVO);

            return "admin/showMaterialApplication";
        }

        //非物资组
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

        return "admin/showMaterialApplication";

    }

    //添加物资申购
    @RequestMapping(value = "/addMaterialApplication", method = {RequestMethod.GET})
    public String addMaterialApplicationUI(Model model) throws Exception {

        return "admin/addMaterialApplication";
    }

    // 添加物资申购
    @RequestMapping(value = "/addMaterialApplication", method = {RequestMethod.POST})
    public String addMaterialApplicationCustom(MaterialApplicationCustom materialApplicationCustom, Model model) throws Exception {

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        ViewEmployeeMiPsd viewEmployeeMiPsd = null;
        try {
            //切换数据源至SQLServer
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MSSQL);
            viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());
            //切换数据源至MySQL
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
        } catch (Exception e) {
            //切换数据源至MySQL(启用备用库)
            try{
                CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
                viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());

            }catch (Exception eSwitch){
                eSwitch.printStackTrace();
            }
            e.printStackTrace();
        }



        //设置申购初始化时间
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);

        //无初始时间时设置初始时间
        if(materialApplicationCustom.getCreateTime() == null || materialApplicationCustom.getCreateTime().length() == 0)
        {
            materialApplicationCustom.setCreateTime(dateString);
        }

        //设置申购初始化状态
        materialApplicationCustom.setFlag(0);
        materialApplicationCustom.setFaultUrgent(0);
        materialApplicationCustom.setGroupVisible(0);
        materialApplicationCustom.setHighApproved(0);
        materialApplicationCustom.setApprovedFlag(0);
        materialApplicationCustom.setHighLeaderFlag1(0);
        materialApplicationCustom.setHighLeaderFlag2(0);
        materialApplicationCustom.setHighLeaderFlag3(0);
        materialApplicationCustom.setHighLeaderApproved1(0);
        materialApplicationCustom.setHighLeaderApproved2(0);
        materialApplicationCustom.setHighLeaderApproved3(0);


        //设置申购所属部门编码
        materialApplicationCustom.setDepartcode(viewEmployeeMiPsd.getDeptCode());

        //设置申购所属人员ID
        materialApplicationCustom.setUserid(viewEmployeeMiPsd.getCode());

        //保存该记录相关数据以便产生推送
        try {
            PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                    "0","21");

            Boolean result = materialApplicationService.saveAndPre(materialApplicationCustom, pushMessage);

            if (!result) {
                model.addAttribute("message", "抱歉，故障信息保存失败");
                return "error";
            }
            //向指定组推送消息
           messagePushUtil.GroupPushSingle(pushMessage,"examiner");
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }

        //重定向
        return "redirect:/admin/showMaterialApplication";
    }

    // 修改物资申购页面显示
    @RequestMapping(value = "/editMaterialApplication", method = {RequestMethod.GET})
    public String editMaterialApplicationUI(Integer id, Model model) throws Exception {
        if (id == null) {
            return "redirect:/admin/showMaterialApplication";
        }
        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        //返回审核组标识
        if(subject.hasRole("examiner")){
            model.addAttribute("examiner", true);
        }else{
            model.addAttribute("examiner", false);
        }
        //返回分管院长标识
        if(subject.hasRole("dpdean")){
            model.addAttribute("dpdean", true);
        }else{
            model.addAttribute("dpdean", false);
        }
        //返回信息主管副院长标识
        if(subject.hasRole("infodean")){
            model.addAttribute("infodean", true);
        }else{
            model.addAttribute("infodean", false);
        }
        //返回院长标识
        if(subject.hasRole("alldean")){
            model.addAttribute("alldean", true);
        }else{
            model.addAttribute("alldean", false);
        }
        MaterialApplication materialApplication = materialApplicationService.findById(id);
        if (materialApplication == null) {
            throw new CustomException("抱歉，未找到该物资申购相关信息");
        }

        model.addAttribute("materialApplication", materialApplication);


        return "admin/editMaterialApplication";
    }

    // 修改物资申购页面处理
    @RequestMapping(value = "/editMaterialApplication", method = {RequestMethod.POST})
    public String editMaterialApplication(MaterialApplicationCustom materialApplicationCustom) throws Exception {

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        ViewEmployeeMiPsd viewEmployeeMiPsd = null;
        try {
            //切换数据源至SQLServer
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MSSQL);
            viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());
            //切换数据源至MySQL
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
        } catch (Exception e) {
            //切换数据源至MySQL(启用备用库)
            try{
                CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
                viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());

            }catch (Exception eSwitch){
                eSwitch.printStackTrace();
            }
            e.printStackTrace();
        }

        materialApplicationCustom.setLeader(viewEmployeeMiPsd.getCode());

        materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);

        //重定向
        return "redirect:/admin/showMaterialApplication";
    }

    // 预推送物资申购页面显示
    @RequestMapping(value = "/prePushMaterialApplication", method = {RequestMethod.GET})
    public String prePushMaterialApplication(HttpServletRequest request, Model modelV) throws Exception {

        Integer material_id = Integer.parseInt(request.getParameter("id"));
        String material_feedback = request.getParameter("feedback");
        String material_brand = request.getParameter("brand");
        String material_model = request.getParameter("model");
        int material_judge = 0;
        int material_total = 0;
        try {
            material_judge = Integer.parseInt(request.getParameter("judge"));
            material_total = Integer.parseInt(request.getParameter("total"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        //获取院领导列表
        List<Role> deans = new ArrayList<>();

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        //非审查组不允许推送
        if(!subject.hasRole("examiner")){
            return "redirect:/admin/showMaterialApplication";
        }
        try {
            deans = roleService.findByDean();
        } catch (Exception e) {
            e.printStackTrace();
            modelV.addAttribute("message", "院领导列表获取失败");
            return "error";
        }
        //不更新,发送到 push 进行处理
        modelV.addAttribute("material_id",material_id);
        modelV.addAttribute("material_feedback",material_feedback);
        modelV.addAttribute("material_brand",material_brand);
        modelV.addAttribute("material_model",material_model);
        modelV.addAttribute("material_judge",material_judge);
        modelV.addAttribute("material_total",material_total);
        modelV.addAttribute("deans", deans);
        return "admin/prePushMaterialApplication";
    }

    // 推送物资申购处理
    @RequestMapping(value = "/pushMaterialApplication")
    public String pushMaterialApplication(HttpServletRequest request,Model modelV) throws Exception {

        Integer id = Integer.parseInt(request.getParameter("material_id"));
        String feedback = request.getParameter("material_feedback");
        String brand = request.getParameter("material_brand");
        String model = request.getParameter("material_model");
        int judge = 0;
        int total = 0;
        try {
            judge = Integer.parseInt(request.getParameter("material_judge"));
            total = Integer.parseInt(request.getParameter("material_total"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        String viceDean = request.getParameter("viceDean");
        String infoVice = request.getParameter("infoVice");
        String dean = request.getParameter("dean");


        if (id == null) {
            return "redirect:/admin/showMaterialApplication";
        }

        //获取当前物资申购问题
        MaterialApplicationCustom materialApplicationCustom = materialApplicationService.findById(id);
        if (materialApplicationCustom == null) {
            throw new CustomException("抱歉，未找到该物资申购相关信息");
        }

        //申购处理完毕不能再推送
        if(materialApplicationCustom.getFlag() == 2){
            modelV.addAttribute("message", "该申购处理完毕，不能再推送");
            return "error";
        }


        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        //非审查组不允许推送
        if(!subject.hasRole("examiner")){
            return "redirect:/admin/showMaterialApplication";
        }
        ViewEmployeeMiPsd viewEmployeeMiPsd = null;
        try {
            //切换数据源至SQLServer
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MSSQL);
            viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());
            //切换数据源至MySQL
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
        } catch (Exception e) {
            //切换数据源至MySQL(启用备用库)
            try{
                CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
                viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());

            }catch (Exception eSwitch){
                eSwitch.printStackTrace();
            }
            e.printStackTrace();
        }



        //更新该物资申购问题数据
        materialApplicationCustom.setBrand(brand);
        materialApplicationCustom.setModel(model);
        materialApplicationCustom.setJudge(judge);
        materialApplicationCustom.setTotal(total);
        materialApplicationCustom.setLeader(viewEmployeeMiPsd.getCode());
        materialApplicationCustom.setXxkyj(feedback);
        materialApplicationCustom.setFlag(1);


        //保存该记录相关数据以便产生推送
        try {
            //创建推送消息
            PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                    "2","24");
            try {
                pushMessageService.save(pushMessage);
            } catch (Exception e) {
                e.printStackTrace();
                return "error";
            }
            //向申报人推送消息
            messagePushUtil.SpecifiedPushSingle(pushMessage,materialApplicationCustom.getUserid());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(!viceDean.equals("-1")){
            //如果指定分管院长，设置院领导审核标识、分管院长审核标识、分管院长ID
            materialApplicationCustom.setHighApproved(1);
            materialApplicationCustom.setHighLeaderId1(viceDean);
            materialApplicationCustom.setHighLeaderApproved1(1);

            //保存该记录相关数据以便产生推送
            try {
                //创建推送消息
                PushMessage pushMessage = createPushUtil.CreatePreMessage(viewEmployeeMiPsd.getCode(),"0","1",
                        "2","25");
                try {
                    pushMessageService.save(pushMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                    return "error";
                }
                //向分管院长推送消息
                messagePushUtil.SpecifiedPushSingle(pushMessage,viceDean);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        if(!infoVice.equals("-1")){
            //如果指定信息主管副院长，设置院领导审核标识、信息主管副院长审核标识、副院长ID
            materialApplicationCustom.setHighApproved(1);
            materialApplicationCustom.setHighLeaderId2(infoVice);
            materialApplicationCustom.setHighLeaderApproved2(1);

            //保存该记录相关数据以便产生推送
            try {
                //创建推送消息
                PushMessage pushMessage = createPushUtil.CreatePreMessage(viewEmployeeMiPsd.getCode(),"0","1",
                        "2","25");
                try {
                    pushMessageService.save(pushMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                    return "error";
                }
                //向信息主管副院长推送消息
                messagePushUtil.SpecifiedPushSingle(pushMessage,infoVice);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        if(!dean.equals("-1")){
            //如果指定院长，设置院领导审核标识、院长审核标识、副院长ID
            materialApplicationCustom.setHighApproved(1);
            materialApplicationCustom.setHighLeaderId3(dean);
            materialApplicationCustom.setHighLeaderApproved3(1);

            //保存该记录相关数据以便产生推送
            try {
                //创建推送消息
                PushMessage pushMessage = createPushUtil.CreatePreMessage(viewEmployeeMiPsd.getCode(),"0","1",
                        "2","25");
                try {
                    pushMessageService.save(pushMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                    return "error";
                }
                //向院长推送消息
                messagePushUtil.SpecifiedPushSingle(pushMessage,dean);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
        //重定向
        return "redirect:/admin/showMaterialApplication";
    }


    // 物资申购审批拒绝
    @RequestMapping(value = "/denyMaterialApplication", method = {RequestMethod.GET})
    public String denyMaterialApplication(HttpServletRequest request) throws Exception {
        Integer id = Integer.parseInt(request.getParameter("id"));
        String feedback = request.getParameter("feedback");
        if (id == null) {
            return "redirect:/admin/showMaterialApplication";
        }
        MaterialApplicationCustom materialApplicationCustom = materialApplicationService.findById(id);
        if (materialApplicationCustom == null) {
            throw new CustomException("抱歉，未找到该物资申购相关信息");
        }
        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        //不是院领导则返回
        if(!subject.hasRole("dpdean") && !subject.hasRole("infodean") && !subject.hasRole("alldean")){
            return "redirect:/admin/showMaterialApplication";
        }
        ViewEmployeeMiPsd viewEmployeeMiPsd = null;
        try {
            //切换数据源至SQLServer
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MSSQL);
            viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());
            //切换数据源至MySQL
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
        } catch (Exception e) {
            //切换数据源至MySQL(启用备用库)
            try{
                CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
                viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());

            }catch (Exception eSwitch){
                eSwitch.printStackTrace();
            }
            e.printStackTrace();
        }

        //保存意见至对应位，保存姓名，单个审批结果标识置2-拒绝，最终审批结果标识置2-拒绝
        //处理状态置2-已完成，
        if(subject.hasRole("dpdean") && materialApplicationCustom.getHighLeaderApproved1() == 1){
            materialApplicationCustom.setApprovedFlag(2);
            materialApplicationCustom.setHighLeaderReback1(feedback);
            materialApplicationCustom.setHighLeaderName1(viewEmployeeMiPsd.getName());
            materialApplicationCustom.setHighLeaderFlag1(2);
            materialApplicationCustom.setFlag(2);
        }else if(subject.hasRole("infodean") && materialApplicationCustom.getHighLeaderApproved2() == 1){
            materialApplicationCustom.setApprovedFlag(2);
            materialApplicationCustom.setHighLeaderReback2(feedback);
            materialApplicationCustom.setHighLeaderName2(viewEmployeeMiPsd.getName());
            materialApplicationCustom.setHighLeaderFlag2(2);
            materialApplicationCustom.setFlag(2);
        }else if(subject.hasRole("alldean") && materialApplicationCustom.getHighLeaderApproved3() == 1){
            materialApplicationCustom.setApprovedFlag(2);
            materialApplicationCustom.setHighLeaderReback3(feedback);
            materialApplicationCustom.setHighLeaderName3(viewEmployeeMiPsd.getName());
            materialApplicationCustom.setHighLeaderFlag3(2);
            materialApplicationCustom.setFlag(2);
        }else{
            return "admin/showMaterialApplication";
        }
        materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
        //保存该记录相关数据以便产生推送
        try {
            //创建推送消息
            PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                    "2","23");
            try {
                pushMessageService.save(pushMessage);
            } catch (Exception e) {
                e.printStackTrace();
                return "error";
            }
            //向申报人推送消息
            messagePushUtil.SpecifiedPushSingle(pushMessage,materialApplicationCustom.getUserid());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:editMaterialApplication?id=" + materialApplicationCustom.getId();
    }

    // 物资申购审批通过
    @RequestMapping(value = "/passMaterialApplication")
    public String passMaterialApplication(HttpServletRequest request) throws Exception {
        Integer id = Integer.parseInt(request.getParameter("id"));
        String feedback = request.getParameter("feedback");
        if (id == null) {
            return "redirect:/admin/showMaterialApplication";
        }
        MaterialApplicationCustom materialApplicationCustom = materialApplicationService.findById(id);
        if (materialApplicationCustom == null) {
            throw new CustomException("抱歉，未找到该物资申购相关信息");
        }
        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        //权限不正确则返回
        if(!subject.hasRole("dpdean") && !subject.hasRole("infodean") && !subject.hasRole("alldean")){
            return "redirect:/admin/showMaterialApplication";
        }
        ViewEmployeeMiPsd viewEmployeeMiPsd = null;
        try {
            //切换数据源至SQLServer
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MSSQL);
            viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());
            //切换数据源至MySQL
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
        } catch (Exception e) {
            //切换数据源至MySQL(启用备用库)
            try{
                CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
                viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());

            }catch (Exception eSwitch){
                eSwitch.printStackTrace();
            }
            e.printStackTrace();
        }

        //保存意见至对应位，保存姓名，单个审批结果标识置1-通过
        if(subject.hasRole("dpdean")){
            materialApplicationCustom.setHighLeaderReback1(feedback);
            materialApplicationCustom.setHighLeaderName1(viewEmployeeMiPsd.getName());
            materialApplicationCustom.setHighLeaderFlag1(1);
            //判断审核者数量以作不同处理
            //如果审核者只有dpdean
            if(0 == materialApplicationCustom.getHighLeaderApproved2()
                && 0 == materialApplicationCustom.getHighLeaderApproved3()){
                //最终审批结果标识置1-通过，使物资处理组可见标识置1-可见
                materialApplicationCustom.setApprovedFlag(1);
                materialApplicationCustom.setGroupVisible(1);
                //保存该记录相关数据以便产生推送
                try {
                    //创建推送消息
                    PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                            "0","26");
                    try {
                        pushMessageService.save(pushMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //向申报人推送消息
                    messagePushUtil.SpecifiedPushSingle(pushMessage,materialApplicationCustom.getUserid());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    //创建推送消息
                    PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                            "0","21");
                    try {
                        pushMessageService.save(pushMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "error";
                    }
                    //向处理组推送消息
                    messagePushUtil.GroupPushSingle(pushMessage,"material");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "redirect:editMaterialApplication?id=" + materialApplicationCustom.getId();

            }else if(1 == (materialApplicationCustom.getHighLeaderApproved2()
                    + materialApplicationCustom.getHighLeaderApproved3())){
                //如果审核者除dpdean外还有一位
                //信息主管院长需审批但未审批
                if(1 == materialApplicationCustom.getHighLeaderApproved2()
                        && 0 == materialApplicationCustom.getHighLeaderFlag2()){
                    //只更新相关数据
                    materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
                }else if(1 == materialApplicationCustom.getHighLeaderApproved3()
                        && 0 == materialApplicationCustom.getHighLeaderFlag3()){
                    //院长需审批但未审批
                    //只更新相关数据
                    materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
                }else{
                    //说明需审核者已审核且结果皆为通过，使物资处理组可见标识置1-可见,
                    // 最终审批结果标识置1-通过，更新数据并向处理组及用户推送消息
                    materialApplicationCustom.setApprovedFlag(1);
                    materialApplicationCustom.setGroupVisible(1);
                    materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
                    //保存该记录相关数据以便产生推送
                    try {
                        //创建推送消息
                        PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                                "0","26");
                        try {
                            pushMessageService.save(pushMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //向申报人推送消息
                        messagePushUtil.SpecifiedPushSingle(pushMessage,materialApplicationCustom.getUserid());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        //创建推送消息
                        PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                                "0","21");
                        try {
                            pushMessageService.save(pushMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return "error";
                        }
                        //向处理组推送消息
                        messagePushUtil.GroupPushSingle(pushMessage,"material");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return "redirect:editMaterialApplication?id=" + materialApplicationCustom.getId();
                }
            }else if(2 == (materialApplicationCustom.getHighLeaderApproved2()
                    + materialApplicationCustom.getHighLeaderApproved3())){
                //如果审核者除dpdean外还有两位
                //如果审核者审批皆通过
                if(2 == (materialApplicationCustom.getHighLeaderFlag2()
                        +  materialApplicationCustom.getHighLeaderFlag3())){
                    //说明需审核者已审核且结果皆为通过，使物资处理组可见标识置1-可见,更新数据并向处理组推送消息//说明需审核者已审核且结果皆为通过，使物资处理组可见标识置1-可见,
                    // 最终审批结果标识置1-通过，更新数据并向处理组及用户推送消息
                    materialApplicationCustom.setApprovedFlag(1);
                    materialApplicationCustom.setGroupVisible(1);
                    materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
                    //保存该记录相关数据以便产生推送
                    try {
                        //创建推送消息
                        PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                                "0","26");
                        try {
                            pushMessageService.save(pushMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //向申报人推送消息
                        messagePushUtil.SpecifiedPushSingle(pushMessage,materialApplicationCustom.getUserid());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        //创建推送消息
                        PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                                "0","21");
                        try {
                            pushMessageService.save(pushMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return "error";
                        }
                        //向处理组推送消息
                        messagePushUtil.GroupPushSingle(pushMessage,"material");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return "redirect:editMaterialApplication?id=" + materialApplicationCustom.getId();
                }else {
                    //只更新相关数据
                    materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
                    return "redirect:editMaterialApplication?id=" + materialApplicationCustom.getId();
                }
            }else{
                return "error";
            }
        }else if(subject.hasRole("infodean")){
            //保存意见至对应位，保存姓名，单个审批结果标识置1-通过
            materialApplicationCustom.setHighLeaderReback2(feedback);
            materialApplicationCustom.setHighLeaderName2(viewEmployeeMiPsd.getName());
            materialApplicationCustom.setHighLeaderFlag2(1);
            //判断审核者数量以作不同处理
            //如果审核者只有infodean
            if(0 == materialApplicationCustom.getHighLeaderApproved1()
                    && 0 == materialApplicationCustom.getHighLeaderApproved3()){
                //最终审批结果标识置1-通过，使物资处理组可见标识置1-可见
                materialApplicationCustom.setApprovedFlag(1);
                materialApplicationCustom.setGroupVisible(1);
                //保存该记录相关数据以便产生推送
                try {
                    //创建推送消息
                    PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                            "0","26");
                    try {
                        pushMessageService.save(pushMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //向申报人推送消息
                    messagePushUtil.SpecifiedPushSingle(pushMessage,materialApplicationCustom.getUserid());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    //创建推送消息
                    PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                            "0","21");
                    try {
                        pushMessageService.save(pushMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "error";
                    }
                    //向处理组推送消息
                    messagePushUtil.GroupPushSingle(pushMessage,"material");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "redirect:editMaterialApplication?id=" + materialApplicationCustom.getId();

            }else if(1 == (materialApplicationCustom.getHighLeaderApproved1()
                    + materialApplicationCustom.getHighLeaderApproved3())){
                //如果审核者除infodean外还有一位
                //分管院长需审批但未审批
                if(1 == materialApplicationCustom.getHighLeaderApproved1()
                        && 0 == materialApplicationCustom.getHighLeaderFlag1()){
                    //只更新相关数据
                    materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
                }else if(1 == materialApplicationCustom.getHighLeaderApproved3()
                        && 0 == materialApplicationCustom.getHighLeaderFlag3()){
                    //院长需审批但未审批
                    //只更新相关数据
                    materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
                }else{
                    //说明需审核者已审核且结果皆为通过，使物资处理组可见标识置1-可见,
                    // 最终审批结果标识置1-通过，更新数据并向处理组及用户推送消息
                    materialApplicationCustom.setApprovedFlag(1);
                    materialApplicationCustom.setGroupVisible(1);
                    materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
                    //保存该记录相关数据以便产生推送
                    try {
                        //创建推送消息
                        PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                                "0","26");
                        try {
                            pushMessageService.save(pushMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //向申报人推送消息
                        messagePushUtil.SpecifiedPushSingle(pushMessage,materialApplicationCustom.getUserid());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        //创建推送消息
                        PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                                "0","21");
                        try {
                            pushMessageService.save(pushMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return "error";
                        }
                        //向处理组推送消息
                        messagePushUtil.GroupPushSingle(pushMessage,"material");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return "redirect:editMaterialApplication?id=" + materialApplicationCustom.getId();
                }
            }else if(2 == (materialApplicationCustom.getHighLeaderApproved1()
                    + materialApplicationCustom.getHighLeaderApproved3())){
                //如果审核者除infodean外还有两位
                //如果审核者审批皆通过
                if(2 == (materialApplicationCustom.getHighLeaderFlag1()
                        +  materialApplicationCustom.getHighLeaderFlag3())){
                    //说明需审核者已审核且结果皆为通过，使物资处理组可见标识置1-可见,更新数据并向处理组推送消息//说明需审核者已审核且结果皆为通过，使物资处理组可见标识置1-可见,
                    // 最终审批结果标识置1-通过，更新数据并向处理组及用户推送消息
                    materialApplicationCustom.setApprovedFlag(1);
                    materialApplicationCustom.setGroupVisible(1);
                    materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
                    //保存该记录相关数据以便产生推送
                    try {
                        //创建推送消息
                        PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                                "0","26");
                        try {
                            pushMessageService.save(pushMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //向申报人推送消息
                        messagePushUtil.SpecifiedPushSingle(pushMessage,materialApplicationCustom.getUserid());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        //创建推送消息
                        PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                                "0","21");
                        try {
                            pushMessageService.save(pushMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return "error";
                        }
                        //向处理组推送消息
                        messagePushUtil.GroupPushSingle(pushMessage,"material");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return "redirect:editMaterialApplication?id=" + materialApplicationCustom.getId();
                }else {
                    //只更新相关数据
                    materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
                    return "redirect:editMaterialApplication?id=" + materialApplicationCustom.getId();
                }
            }else{
                return "error";
            }


        }else if(subject.hasRole("alldean")){
            materialApplicationCustom.setHighLeaderReback3(feedback);
            materialApplicationCustom.setHighLeaderName3(viewEmployeeMiPsd.getName());
            materialApplicationCustom.setHighLeaderFlag3(1);
            //判断审核者数量以作不同处理
            //如果审核者只有alldean
            if(0 == materialApplicationCustom.getHighLeaderApproved1()
                    && 0 == materialApplicationCustom.getHighLeaderApproved2()){
                //最终审批结果标识置1-通过，使物资处理组可见标识置1-可见
                materialApplicationCustom.setApprovedFlag(1);
                materialApplicationCustom.setGroupVisible(1);
                //保存该记录相关数据以便产生推送
                try {
                    //创建推送消息
                    PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                            "0","26");
                    try {
                        pushMessageService.save(pushMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //向申报人推送消息
                    messagePushUtil.SpecifiedPushSingle(pushMessage,materialApplicationCustom.getUserid());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    //创建推送消息
                    PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                            "0","21");
                    try {
                        pushMessageService.save(pushMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "error";
                    }
                    //向处理组推送消息
                    messagePushUtil.GroupPushSingle(pushMessage,"material");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "redirect:editMaterialApplication?id=" + materialApplicationCustom.getId();

            }else if(1 == (materialApplicationCustom.getHighLeaderApproved1()
                    + materialApplicationCustom.getHighLeaderApproved2())){
                //如果审核者除alldean外还有一位
                //分管院长需审批但未审批
                if(1 == materialApplicationCustom.getHighLeaderApproved1()
                        && 0 == materialApplicationCustom.getHighLeaderFlag1()){
                    //只更新相关数据
                    materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
                }else if(1 == materialApplicationCustom.getHighLeaderApproved2()
                        && 0 == materialApplicationCustom.getHighLeaderFlag2()){
                    //信息主管副院长需审批但未审批
                    //只更新相关数据
                    materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
                }else{
                    //说明需审核者已审核且结果皆为通过，使物资处理组可见标识置1-可见,
                    // 最终审批结果标识置1-通过，更新数据并向处理组及用户推送消息
                    materialApplicationCustom.setApprovedFlag(1);
                    materialApplicationCustom.setGroupVisible(1);
                    materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
                    //保存该记录相关数据以便产生推送
                    try {
                        //创建推送消息
                        PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                                "0","26");
                        try {
                            pushMessageService.save(pushMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //向申报人推送消息
                        messagePushUtil.SpecifiedPushSingle(pushMessage,materialApplicationCustom.getUserid());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        //创建推送消息
                        PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                                "0","21");
                        try {
                            pushMessageService.save(pushMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return "error";
                        }
                        //向处理组推送消息
                        messagePushUtil.GroupPushSingle(pushMessage,"material");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return "redirect:editMaterialApplication?id=" + materialApplicationCustom.getId();
                }
            }else if(2 == (materialApplicationCustom.getHighLeaderApproved1()
                    + materialApplicationCustom.getHighLeaderApproved2())){
                //如果审核者除alldean外还有两位
                //如果审核者审批皆通过
                if(2 == (materialApplicationCustom.getHighLeaderFlag1()
                        +  materialApplicationCustom.getHighLeaderFlag2())){
                    //说明需审核者已审核且结果皆为通过，使物资处理组可见标识置1-可见,更新数据并向处理组推送消息//说明需审核者已审核且结果皆为通过，使物资处理组可见标识置1-可见,
                    // 最终审批结果标识置1-通过，更新数据并向处理组及用户推送消息
                    materialApplicationCustom.setApprovedFlag(1);
                    materialApplicationCustom.setGroupVisible(1);
                    materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
                    //保存该记录相关数据以便产生推送
                    try {
                        //创建推送消息
                        PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                                "0","26");
                        try {
                            pushMessageService.save(pushMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //向申报人推送消息
                        messagePushUtil.SpecifiedPushSingle(pushMessage,materialApplicationCustom.getUserid());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        //创建推送消息
                        PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                                "0","21");
                        try {
                            pushMessageService.save(pushMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return "error";
                        }
                        //向处理组推送消息
                        messagePushUtil.GroupPushSingle(pushMessage,"material");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return "redirect:editMaterialApplication?id=" + materialApplicationCustom.getId();
                }else {
                    //只更新相关数据
                    materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
                    return "redirect:editMaterialApplication?id=" + materialApplicationCustom.getId();
                }
            }else{
                return "error";
            }

        }else{
            return "redirect:editMaterialApplication?id=" + materialApplicationCustom.getId();
        }


        return "admin/showMaterialApplication";
    }


    // 处理物资申购
    @RequestMapping(value = "/dealMaterialApplication")
    public String dealMaterialApplication(HttpServletRequest request) throws Exception {

        Integer id = Integer.parseInt(request.getParameter("id"));
        String feedback = request.getParameter("feedback");
        String brand = request.getParameter("brand");
        String model = request.getParameter("model");
        int judge = 0;
        int total = 0;
        try {
            judge = Integer.parseInt(request.getParameter("judge"));
            total = Integer.parseInt(request.getParameter("total"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }


        if (id == null) {
            return "redirect:/admin/showMaterialApplication";
        }

        //获取当前物资申购问题
        MaterialApplicationCustom materialApplicationCustom = materialApplicationService.findById(id);
        if (materialApplicationCustom == null) {
            throw new CustomException("抱歉，未找到该物资申购相关信息");
        }

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        ViewEmployeeMiPsd viewEmployeeMiPsd = null;
        try {
            //切换数据源至SQLServer
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MSSQL);
            viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());
            //切换数据源至MySQL
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
        } catch (Exception e) {
            //切换数据源至MySQL(启用备用库)
            try{
                CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
                viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());

            }catch (Exception eSwitch){
                eSwitch.printStackTrace();
            }
            e.printStackTrace();
        }
        if(materialApplicationCustom.getFlag() == 0 || materialApplicationCustom.getFlag() == 1){
            //更新该物资申购问题数据
            materialApplicationCustom.setBrand(brand);
            materialApplicationCustom.setModel(model);
            materialApplicationCustom.setJudge(judge);
            materialApplicationCustom.setTotal(total);
            materialApplicationCustom.setLeader(viewEmployeeMiPsd.getCode());

            //审查组审查后使处理组可见,并保留审查组意见,并向处理组及用户推送消息
            if(subject.hasRole("examiner") && materialApplicationCustom.getFlag() == 0){
                materialApplicationCustom.setFlag(1);
                materialApplicationCustom.setGroupVisible(1);
                materialApplicationCustom.setXxkyj(feedback);
                materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
                //保存该记录相关数据以便产生推送
                try {
                    //创建推送消息
                    PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                            "0","21");
                    try {
                        pushMessageService.save(pushMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "error";
                    }
                    //向处理组推送消息
                    messagePushUtil.GroupPushSingle(pushMessage,"material");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    //创建推送消息
                    PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                            "2","22");
                    try {
                        pushMessageService.save(pushMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "error";
                    }
                    //向申报人推送消息
                    messagePushUtil.SpecifiedPushSingle(pushMessage,materialApplicationCustom.getUserid());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "redirect:editMaterialApplication?id=" + materialApplicationCustom.getId();
            }else{
                materialApplicationCustom.setFlag(1);
            }
            //将反馈插入空白反馈字段
            if(materialApplicationCustom.getFeedbackId1() == null){
                materialApplicationCustom.setFeedbackContent1(feedback);
                materialApplicationCustom.setFeedbackId1(viewEmployeeMiPsd.getCode());
                materialApplicationCustom.setFeedbackName1(viewEmployeeMiPsd.getName());
            }else if(materialApplicationCustom.getFeedbackId2() == null){
                materialApplicationCustom.setFeedbackContent2(feedback);
                materialApplicationCustom.setFeedbackId2(viewEmployeeMiPsd.getCode());
                materialApplicationCustom.setFeedbackName2(viewEmployeeMiPsd.getName());
            }else if(materialApplicationCustom.getFeedbackId3() == null){
                materialApplicationCustom.setFeedbackContent3(feedback);
                materialApplicationCustom.setFeedbackId3(viewEmployeeMiPsd.getCode());
                materialApplicationCustom.setFeedbackName3(viewEmployeeMiPsd.getName());
            }else if(materialApplicationCustom.getFeedbackId4() == null){
                materialApplicationCustom.setFeedbackContent4(feedback);
                materialApplicationCustom.setFeedbackId4(viewEmployeeMiPsd.getCode());
                materialApplicationCustom.setFeedbackName4(viewEmployeeMiPsd.getName());
            }else if(materialApplicationCustom.getFeedbackId5() == null){
                materialApplicationCustom.setFeedbackContent5(feedback);
                materialApplicationCustom.setFeedbackId5(viewEmployeeMiPsd.getCode());
                materialApplicationCustom.setFeedbackName5(viewEmployeeMiPsd.getName());
            }

            materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
            //保存该记录相关数据以便产生推送
            try {
                //创建推送消息
                PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                        "2","22");
                try {
                    pushMessageService.save(pushMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                    return "error";
                }
                //向申报人推送消息
                messagePushUtil.SpecifiedPushSingle(pushMessage,materialApplicationCustom.getUserid());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return "redirect:editMaterialApplication?id=" + materialApplicationCustom.getId();
    }

    // 物资申购处理完成
    @RequestMapping(value = "/completeMaterialApplication")
    public String completeMaterialApplication(HttpServletRequest request) throws Exception {

        Integer id = Integer.parseInt(request.getParameter("id"));
        String feedback = request.getParameter("feedback");
        String brand = request.getParameter("brand");
        String model = request.getParameter("model");
        int judge = 0;
        int total = 0;
        try {
            judge = Integer.parseInt(request.getParameter("judge"));
            total = Integer.parseInt(request.getParameter("total"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }


        if (id == null) {
            return "redirect:/admin/showMaterialApplication";
        }

        //获取当前物资申购信息
        MaterialApplicationCustom materialApplicationCustom = materialApplicationService.findById(id);
        if (materialApplicationCustom == null) {
            throw new CustomException("抱歉，未找到该物资申购相关信息");
        }

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        ViewEmployeeMiPsd viewEmployeeMiPsd = null;
        try {
            //切换数据源至SQLServer
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MSSQL);
            viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());
            //切换数据源至MySQL
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
        } catch (Exception e) {
            //切换数据源至MySQL(启用备用库)
            try{
                CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
                viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());

            }catch (Exception eSwitch){
                eSwitch.printStackTrace();
            }
            e.printStackTrace();
        }
        if(materialApplicationCustom.getFlag() == 1 || subject.hasRole("examiner")){
            //更新该物资申购问题数据
            materialApplicationCustom.setFlag(2);
            materialApplicationCustom.setLeader(viewEmployeeMiPsd.getCode());
            materialApplicationCustom.setReback(feedback);
            materialApplicationCustom.setBrand(brand);
            materialApplicationCustom.setModel(model);
            materialApplicationCustom.setJudge(judge);
            materialApplicationCustom.setTotal(total);
            materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);
            //保存该记录相关数据以便产生推送
            try {
                //创建推送消息
                PushMessage pushMessage = createPushUtil.CreatePreMessage(materialApplicationCustom.getUserid(),"0","1",
                        "2","23");
                try {
                    pushMessageService.save(pushMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                    return "error";
                }
                //向申报人推送消息
                messagePushUtil.SpecifiedPushSingle(pushMessage,materialApplicationCustom.getUserid());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return "redirect:editMaterialApplication?id=" + materialApplicationCustom.getId();
    }

    // 查看物资申购详情
    @RequestMapping(value = "/checkMaterialApplication", method = {RequestMethod.GET})
    public String checkMaterialApplication(Integer id, Model model) throws Exception {
        if (id == null) {
            return "redirect:/admin/showMaterialApplication";
        }
        MaterialApplication materialApplication = materialApplicationService.findById(id);
        if (materialApplication == null) {
            throw new CustomException("抱歉，未找到该物资申购相关信息");
        }

        model.addAttribute("materialApplication", materialApplication);


        return "admin/checkMaterialApplication";
    }

    // 查看物资申购详情
    @RequestMapping(value = "/checkMaterialApplication", method = {RequestMethod.POST})
    public String checkMaterialApplication(MaterialApplicationCustom materialApplicationCustom) throws Exception {

        materialApplicationService.updataById(materialApplicationCustom.getId(), materialApplicationCustom);

        //重定向
        return "redirect:/admin/showMaterialApplication";
    }

    //搜索物资申购
    @RequestMapping(value = "/searchMaterialApplication")
    private String searchMaterialApplication(String findByDept,String findByName,String findByFlag, Model model) throws Exception {

        List<MaterialApplicationCustom> list = null;
        Map<String, Object> map =new HashMap<String, Object>();
        map.put("dept",findByDept);
        map.put("applicant",findByName);
        int flag = 0;
        try {
            flag = Integer.parseInt(findByFlag);
        } catch (NumberFormatException e) {
            flag = 3;
        }
        map.put("flag",flag);
        try {
            list = materialApplicationService.paginationOfSearchResults(map);
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }

        model.addAttribute("materialApplicationList", list);
        return "admin/showMaterialApplication";
    }

    // 打印物资申购表单
    @RequestMapping(value = "/printMaterialApplication", method = {RequestMethod.GET})
    public String printMaterialApplication(Integer id, Model model) throws Exception {
        if (id == null) {
            return "redirect:/admin/showMaterialApplication";
        }
        MaterialApplication materialApplication = materialApplicationService.findById(id);
        if (materialApplication == null) {
            throw new CustomException("抱歉，未找到该物资申购相关信息");
        }

        model.addAttribute("materialApplication", materialApplication);


        return "admin/printMaterialApplication";
    }

    // 打印物资申购表单
    @RequestMapping(value = "/printMaterialApplication", method = {RequestMethod.POST})
    public String printMaterialApplication(MaterialApplicationCustom materialApplicationCustom) throws Exception {

        //重定向
        return "admin/printMaterialApplication";
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

        //巡检审核者标识
        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        if(subject.hasRole("examiner")){
            model.addAttribute("examiner", 1);
        }else {
            model.addAttribute("examiner", 0);
        }

        model.addAttribute("engineRoomInspectionList", list);
        model.addAttribute("pagingVO", pagingVO);

        return "admin/showEngineRoomInspection";

    }

    //添加机房巡检
    @RequestMapping(value = "/addEngineRoomInspection", method = {RequestMethod.GET})
    public String addEngineRoomInspectionUI(Model model) throws Exception {

        return "admin/addEngineRoomInspection";
    }

    // 添加机房巡检
    @RequestMapping(value = "/addEngineRoomInspection", method = {RequestMethod.POST})
    public String addEngineRoomInspectionCustom(EngineRoomInspectionCustom engineRoomInspectionCustom, Model model) throws Exception {

        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        ViewEmployeeMiPsd viewEmployeeMiPsd = null;
        try {
            //切换数据源至SQLServer
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MSSQL);
            viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());
            //切换数据源至MySQL
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
        } catch (Exception e) {
            //切换数据源至MySQL(启用备用库)
            try{
                CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
                viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());

            }catch (Exception eSwitch){
                eSwitch.printStackTrace();
            }
            e.printStackTrace();
        }



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
        engineRoomInspectionCustom.setUserid(viewEmployeeMiPsd.getCode());
        engineRoomInspectionCustom.setFlag(0);


        //保存该记录相关数据以便产生推送
        try {
            PushMessage pushMessage = createPushUtil.CreatePreMessage(engineRoomInspectionCustom.getUserid(),"0","2",
                    "0","31");
            Boolean result = engineRoomInspectionService.saveAndPre(engineRoomInspectionCustom, pushMessage);

            if (!result) {
                model.addAttribute("message", "抱歉，机房巡检信息保存失败");
                return "error";
            }
            //向管理组推送消息
           messagePushUtil.GroupPushSingle(pushMessage,"examiner");
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }


        //重定向
        return "redirect:/admin/showEngineRoomInspection";
    }

    // 修改机房巡检页面显示
    @RequestMapping(value = "/editEngineRoomInspection", method = {RequestMethod.GET})
    public String editEngineRoomInspectionUI(Integer id, Model model) throws Exception {

        //巡检审核者标识
        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        if(subject.hasRole("examiner")){

        }else {
            return "redirect:/admin/showEngineRoomInspection";
        }

        if (id == null) {
            return "redirect:/admin/showEngineRoomInspection";
        }
        EngineRoomInspection engineRoomInspection = engineRoomInspectionService.findById(id);
        if (engineRoomInspection == null) {
            throw new CustomException("抱歉，未找到该机房巡检相关信息");
        }


        model.addAttribute("engineRoomInspection", engineRoomInspection);


        return "admin/editEngineRoomInspection";
    }

    // 修改机房巡检页面处理
    @RequestMapping(value = "/editEngineRoomInspection", method = {RequestMethod.POST})
    public String editEngineRoomInspection(EngineRoomInspectionCustom engineRoomInspectionCustom) throws Exception {

        //重定向
        return "redirect:/admin/showEngineRoomInspection";
    }

    // 机房巡检审核通过
    @RequestMapping(value = "/passEngineRoomInspection")
    public String dealEngineRoomInspection(HttpServletRequest request) throws Exception {

        //巡检审核者标识
        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        if(subject.hasRole("examiner")){

        }else {
            return "redirect:/admin/showEngineRoomInspection";
        }

        Integer id = Integer.parseInt(request.getParameter("id"));


        if (id == null) {
            return "redirect:/admin/showEngineRoomInspection";
        }

        //获取当前机房巡检问题
        EngineRoomInspectionCustom engineRoomInspectionCustom = engineRoomInspectionService.findById(id);
        if (engineRoomInspectionCustom == null) {
            throw new CustomException("抱歉，未找到该机房巡检相关信息");
        }

        if(engineRoomInspectionCustom.getFlag() != 0){
            return "redirect:checkEngineRoomInspection?id=" + engineRoomInspectionCustom.getId();
        }
        //获取当前操作用户对象
        engineRoomInspectionCustom.setFlag(2);
        engineRoomInspectionService.updataById(engineRoomInspectionCustom.getId(), engineRoomInspectionCustom);

        return "redirect:checkEngineRoomInspection?id=" + engineRoomInspectionCustom.getId();
    }

    // 机房巡检审核拒绝
    @RequestMapping(value = "/denyEngineRoomInspection")
    public String completeEngineRoomInspection(HttpServletRequest request) throws Exception {

        //巡检审核者标识
        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        if(subject.hasRole("examiner")){

        }else {
            return "redirect:/admin/showEngineRoomInspection";
        }

        Integer id = Integer.parseInt(request.getParameter("id"));


        if (id == null) {
            return "redirect:/admin/showEngineRoomInspection";
        }

        //获取当前机房巡检信息
        EngineRoomInspectionCustom engineRoomInspectionCustom = engineRoomInspectionService.findById(id);
        if (engineRoomInspectionCustom == null) {
            throw new CustomException("抱歉，未找到该机房巡检相关信息");
        }

        if(engineRoomInspectionCustom.getFlag() != 0){
            return "redirect:checkEngineRoomInspection?id=" + engineRoomInspectionCustom.getId();
        }
        engineRoomInspectionCustom.setFlag(1);
        engineRoomInspectionService.updataById(engineRoomInspectionCustom.getId(), engineRoomInspectionCustom);


        return "redirect:checkEngineRoomInspection?id=" + engineRoomInspectionCustom.getId();
    }

    // 查看机房巡检详情
    @RequestMapping(value = "/checkEngineRoomInspection", method = {RequestMethod.GET})
    public String checkEngineRoomInspection(Integer id, Model model) throws Exception {
        if (id == null) {
            return "redirect:/admin/showEngineRoomInspection";
        }
        EngineRoomInspection engineRoomInspection = engineRoomInspectionService.findById(id);
        if (engineRoomInspection == null) {
            throw new CustomException("抱歉，未找到该机房巡检相关信息");
        }

        model.addAttribute("engineRoomInspection", engineRoomInspection);


        return "admin/checkEngineRoomInspection";
    }

    // 查看机房巡检详情
    @RequestMapping(value = "/checkEngineRoomInspection", method = {RequestMethod.POST})
    public String checkEngineRoomInspection(EngineRoomInspectionCustom engineRoomInspectionCustom) throws Exception {


        //重定向
        return "redirect:/admin/showEngineRoomInspection";
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
        return "admin/showEngineRoomInspection";
    }

    // 打印物资申购表单
    @RequestMapping(value = "/printEngineRoomInspection", method = {RequestMethod.GET})
    public String printEngineRoomInspection(Integer id, Model model) throws Exception {
        if (id == null) {
            return "redirect:/admin/showEngineRoomInspection";
        }
        EngineRoomInspection engineRoomInspection = engineRoomInspectionService.findById(id);
        if (engineRoomInspection == null) {
            throw new CustomException("抱歉，未找到该物资申购相关信息");
        }

        model.addAttribute("engineRoomInspection", engineRoomInspection);


        return "admin/printEngineRoomInspection";
    }

    // 打印物资申购表单
    @RequestMapping(value = "/printEngineRoomInspection", method = {RequestMethod.POST})
    public String printEngineRoomInspection(EngineRoomInspection engineRoomInspection) throws Exception {

        //重定向
        return "admin/printEngineRoomInspection";
    }
    /*<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<JSON数据获取>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>*/

    //返回操作人相关基本信息JSON
    @RequestMapping(value = "/getApplicantInfo")
    @ResponseBody
    private Map<String, Object> getApplicantInfo() throws Exception {


        //获取当前操作用户对象
        Subject subject = SecurityUtils.getSubject();
        ViewEmployeeMiPsd viewEmployeeMiPsd = null;
        Map<String, Object> map =new HashMap<String, Object>();
        try {
            //切换数据源至SQLServer
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MSSQL);
            viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());
            //切换数据源至MySQL
            CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
        } catch (Exception e) {
            //切换数据源至MySQL(启用备用库)
            try{
                CustomerContextHolder.setCustomerType(CustomerContextHolder.DATA_SOURCE_MYSQL);
                viewEmployeeMiPsd = viewEmployeeMiPsdService.findByCode((String) subject.getPrincipal());

            }catch (Exception eSwitch){
                eSwitch.printStackTrace();
            }
            e.printStackTrace();
        }
        map.put("appliName",viewEmployeeMiPsd.getName());
        map.put("appliDept",viewEmployeeMiPsd.getDeptName());
        map.put("appliDeptCode",viewEmployeeMiPsd.getDeptCode());
        return map;

    }

    //返回电脑故障类型列表JSON
    @RequestMapping(value = "/getProblemsTypeList")
    @ResponseBody
    private Map<String, Object> getProblemsTypeList(String mainType) throws Exception {
        Map<String, Object> map =new HashMap<String, Object>();
        List<ComputerProblemsType> computerProblemsTypeList = new ArrayList<>();
        try {
            computerProblemsTypeList = computerProblemsTypeService.getSecondaryList(mainType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        map.put("computerProblemsTypeList",computerProblemsTypeList);
        return map;
    }

    //返回物资申购类型列表JSON
    @RequestMapping(value = "/getMaterialTypeList")
    @ResponseBody
    private Map<String, Object> getMaterialTypeList() throws Exception {

        Map<String, Object> map =new HashMap<String, Object>();
        List<MaterialApplicationType> materialApplicationTypeList = new ArrayList<>();
        try {
            materialApplicationTypeList = materialApplicationTypeService.getAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
        map.put("materialApplicationTypeList",materialApplicationTypeList);
        return map;

    }

}
