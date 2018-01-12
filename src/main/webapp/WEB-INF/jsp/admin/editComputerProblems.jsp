<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html>
<html>
<head>
    <title></title>

    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <!-- 引入bootstrap -->
    <link rel="stylesheet" type="text/css" href="/css/bootstrap.min.css">
    <!-- 引入JQuery  bootstrap.js-->
    <script src="/js/jquery-3.2.1.min.js"></script>
    <script src="/js/bootstrap.min.js"></script>
</head>
<!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
<!--[if lt IE 9]>
<meta http-equiv="X-UA-Compatible" content="IE=edge,Chrome=1"/>
<meta http-equiv="X-UA-Compatible" content="IE=9"/>
<script type="text/javascript" src="/js/jquery-1.11.1.min.js"></script>
<script src="/js/html5shiv.min.js"></script>
<script src="/js/respond.min.js"></script>
<script type="text/javascript" src="/js/jquery.placeholder.js"></script>
<![endif]-->
<!--[if IE 9]>
<script type="text/javascript" src="/js/jquery.placeholder.js"></script>
<![endif]-->
<body>
<!-- 顶栏 -->
<jsp:include page="top.jsp"></jsp:include>
<!-- 中间主体 -->
<div class="container" id="content">
    <div class="row">
        <jsp:include page="menu.jsp"></jsp:include>
        <div class="col-md-10">
            <div class="panel panel-default">
                <div class="panel-heading">
                    <div class="row">
                        <h2 style="text-align: center;">${computerProblems.title}</h2>
                    </div>
                </div>
                <div class="panel-body">
                    <form class="form-horizontal" role="form" action="/admin/editComputerProblems" id="editfrom"
                          method="post">
                        <div class="form-group">
                            <label class="col-sm-2 control-label">故障类型：</label>
                            <div class="col-sm-8">
                                <input type="text" class="form-control" id="title" name="title" readonly="readonly"
                                       value="${computerProblems.title}">
                            </div>
                        </div>
                        <div class="form-group">
                            <label class="col-sm-2 control-label">科室：</label>
                            <div class="col-sm-8">
                                <input type="text" class="form-control" id="dept" name="dept" readonly="readonly"
                                       value="${computerProblems.dept}">
                            </div>
                        </div>
                        <div class="form-group">
                            <label class="col-sm-2 control-label">申报人：</label>
                            <div class="col-sm-8">
                                <input type="text" class="form-control" id="name" name="name" readonly="readonly"
                                       value="${computerProblems.name}">
                            </div>
                        </div>
                        <div class="form-group">
                            <label class="col-sm-2 control-label">联系方式：</label>
                            <div class="col-sm-8">
                                <input type="text" class="form-control" id="tel" name="tel" readonly="readonly"
                                       value="${computerProblems.tel}">
                            </div>
                        </div>
                        <div id="textareadetail" class="form-group">
                            <label class="col-sm-2 control-label">详情描述：</label>
                            <div class="col-sm-8">
                                <input type="text" class="form-control" name="detail" readonly="readonly"
                                       value="${computerProblems.detail}">
                            </div>
                        </div>
                        <div id="textareareback" class="form-group">
                            <label class="col-sm-2 control-label">反馈：</label>
                            <div class="col-sm-8">
                                <input type="text" class="form-control" id="feedback" name="feedback"
                                       placeholder="请输入反馈"
                                       value="${computerProblems.reback}">
                            </div>
                        </div>
                        <div class="form-group">
                            <label class="col-sm-2 control-label">上传图片：</label>
                            <div class="col-sm-6">
                                <img src="${pageContext.request.contextPath}/upload/${computerProblems.img}"
                                     id="uploadImg"/>
                            </div>
                        </div>


                    </form>
                    <div class="form-group" style="text-align: center">
                        <button class="btn btn-default" type="button" id="dealBtn">处理</button>
                        <button class="btn btn-default" type="button" id="completeBtn">完成</button>
                        <button class="btn btn-default" type="button" id="returnListBtn">返回</button>
                    </div>
                </div>

            </div>

        </div>
    </div>
</div>
<div class="container" id="footer">
    <div class="row">
        <div class="col-md-12"></div>
    </div>
</div>
</body>
<script type="text/javascript">
    $("#nav li:nth-child(1)").addClass("active");

    //处理按钮点击
    $('#dealBtn').on('click', function () {
        var feedback = document.getElementById("feedback").value;
        window.location.href = encodeURI("/admin/dealComputerProblems?id=${computerProblems.id}&feedback=" + feedback);
    });

    //完成按钮点击
    $('#completeBtn').on('click', function () {
        var feedback = document.getElementById("feedback").value;
        window.location.href = encodeURI("/admin/completeComputerProblems?id=${computerProblems.id}&feedback=" + feedback);
    });

    //返回按钮点击
    $('#returnListBtn').on('click', function () {
        window.location.href = "/admin/showComputerProblems";
    });

    //设置图片最大尺寸
    function setImgMaxSize() {
        var uploadImg = document.getElementById('uploadImg');
        if (uploadImg.width > 600) {
            uploadImg.height = uploadImg.height * 600 / uploadImg.width;
            uploadImg.width = 600;
        }
    }

    setImgMaxSize();

</script>
</html>