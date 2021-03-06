package controllers


import _root_.utils.Utils
import play.api.mvc.{Action, Controller}
import java.io.File
import java.util.regex.Pattern
import play.api.libs.json.Json
import net.coobird.thumbnailator.Thumbnails
import controllers.users.Users
import java.net.URL
import play.api.data.Form
import play.api.data.Forms._
import models.user.dao.UserDao

/**
 * Created by zuosanshao.
 * email:zuosanshao@qq.com
 * Date: 12-10-12
 * Time: 上午9:36
 * ***********************
 * description:用于类的说明
 */

object Upload extends Controller{
  val picForm =Form(
    tuple(
      "thumb-path"-> nonEmptyText,
      "area-x1"-> number,
      "area-y1"-> number,
      "area-x2"-> number,
      "area-y2"-> number
    )
  )

  def ajaxImage=Action(parse.multipartFormData) { request =>
    request.body.file("fileData").map { picture =>
      val filename =System.currentTimeMillis()+ picture.filename.substring(picture.filename.lastIndexOf("."))
      if(Utils.isImage(filename)){
        picture.ref.moveTo(new File("/opt/static/images/adv/"+filename),true)
        val src ="/images/adv/"+filename
        Ok(Json.obj("code"->"100","message"->"success","src"->src))

      }else{
        Ok(Json.obj("code"->"104","message"->"fail"))
      }

    }.getOrElse {
      Ok("File uploaded error")
    }
  }

  /* upload img for  editor*/
  def uploadEditorPic =Action(parse.multipartFormData)  {   request =>
    request.body.file("filedata").map { picture =>
     val filename =System.currentTimeMillis()+ picture.filename.substring(picture.filename.lastIndexOf("."))
      if(Utils.isImage(filename)){
        picture.ref.moveTo(new File("/opt/static/images/editor/"+filename),true)
        val picSrc ="/images/editor/"+filename
        Ok(views.html.common.uploadSmeiteEditorPic(picSrc))

      }else{
        Ok(Json.obj("code"->"104","filelink"->"亲，服务器欧巴桑了，请重试"))
      }

    }.getOrElse {
      Ok(Json.obj("code"->"104","filelink"->"亲，服务器欧巴桑了，请重试"))
    }
  }


  /* upload img for  editor*/
  def uploadUEditorPic =Action(parse.multipartFormData)  {   request =>
    request.body.file("fileData").map { picture =>
      val filename =System.currentTimeMillis()+ picture.filename.substring(picture.filename.lastIndexOf("."))
      if(Utils.isImage(filename)){
        picture.ref.moveTo(new File("/opt/static/images/temp/"+filename),true)
        val picSrc ="/images/temp/"+filename
        Ok(Json.obj("url"->picSrc,"title"->"食美特","state"->"SUCCESS"))

      }else{
        Ok(Json.obj("code"->"104","filelink"->"亲，服务器欧巴桑了，请重试"))
      }

    }.getOrElse {
      Ok(Json.obj("code"->"104","filelink"->"亲，服务器欧巴桑了，请重试"))
    }
  }

  def uploadRedactorPic =Action(parse.multipartFormData)  {   request =>
    request.body.file("file").map { picture =>
      val filename =System.currentTimeMillis()+ picture.filename.substring(picture.filename.lastIndexOf("."))
      if(Utils.isImage(filename)){
        picture.ref.moveTo(new File("/opt/static/images/adv/"+filename),true)
        val picSrc ="/images/adv/"+filename
        Ok(Json.obj("filelink"->picSrc))

      }else{
        Ok(Json.obj("err"->"104","msg"->"亲，服务器欧巴桑了，请重试"))
      }

    }.getOrElse {
      Ok(Json.obj("err"->"104","msg"->"亲，服务器欧巴桑了，请重试"))
    }
  }
  /*upload picture for theme style background image*/
  def uploadThemeStylePic = Action(parse.multipartFormData) { request =>
    val picType=request.body.asFormUrlEncoded.get("picType").get.head
    request.body.file("pic").map { picture =>
      val filename =System.currentTimeMillis()+ picture.filename.substring(picture.filename.lastIndexOf("."))
      if(Utils.isImage(filename)){
        picture.ref.moveTo(new File("/opt/static/images/theme/"+filename),true)
        val picSrc ="/images/theme/"+filename
        Ok(views.html.common.uploadThemeStylePicSuccess("100",picType,picSrc))
      }else{
        Ok(views.html.common.uploadThemeStylePicSuccess("101",picType,"none"))
      }

    }.getOrElse {
      Ok(views.html.common.uploadThemeStylePicSuccess("101",picType,"none"))
    }
  }

  /*上传 用户头像图片  */
  def uploadImageSelectPic =Action(parse.multipartFormData)  {   request =>
    request.body.file("filedata").map { picture =>

      val filename =System.currentTimeMillis()+ picture.filename.substring(picture.filename.lastIndexOf("."))
      if(Utils.isImage(filename)){
   picture.ref.moveTo(new File("/opt/static/images/temp/"+filename),true)
     Thumbnails.of(new File("/opt/static/images/temp/"+filename)).size(300,300).toFile(new File("/opt/static/images/temp/"+filename))

        val picSrc ="/images/temp/"+filename
        Ok(views.html.common.uploadImageSelectSuccess(true,picSrc))
      }else{
        Ok(views.html.common.uploadImageSelectSuccess(false,"亲，服务器欧巴桑了，请重试"))
      }

    }.getOrElse {
      Ok(views.html.common.uploadImageSelectSuccess(false,"亲，服务器欧巴桑了，请重试"))
    }
  }

  /*上传图片，用户截取图片    */
  def doUploadUserPic =Users.UserAction {   user => implicit request =>
    picForm.bindFromRequest.fold(
      formWithErrors => BadRequest("something is wrong")  ,
      fields =>{
        val   picName:String=fields._1.substring(fields._1.lastIndexOf("/"))
       val src ="/opt/static"+fields._1

    Thumbnails.of(src).sourceRegion(fields._2,fields._3,(fields._4-fields._2),(fields._5-fields._3)).size((fields._4-fields._2),(fields._5-fields._3)).toFile("/opt/static/images/user/"+picName)

        val picSrc:String= "/images/user/"+picName
        UserDao.modifyPic(user.get.id.get,picSrc)
        Ok(Json.obj("code"->"100","src"->picSrc))
      }
    )
  }

  /*上传图片，用户截取图片    */
  def uploadSitePic=Action(parse.multipartFormData) { request =>
    request.body.file("fileData").map { picture =>

      val filename =System.currentTimeMillis()+ picture.filename.substring(picture.filename.lastIndexOf("."))

      if(Utils.isImage(filename)){
        picture.ref.moveTo(new File("/opt/static/images/temp/"+filename),true)
        Thumbnails.of(new File("/opt/static/images/temp/"+filename)).size(300,300).toFile(new File("/opt/static/images/site/"+filename))
        val src ="/images/site/"+filename
        Ok(Json.obj("code"->"100","msg"->"success","src"->src))

      }else{
        Ok(Json.obj("code"->"104","msg"->"fail"))
      }

    }.getOrElse {
      Ok("File uploaded error")
    }
  }


  /*上传图片，用户截取图片     */
  def doUploadSitePic =Users.UserAction {   user => implicit request =>
    picForm.bindFromRequest.fold(
      formWithErrors => BadRequest("something is wrong")  ,
      fields =>{
        val   picName:String=fields._1.substring(fields._1.lastIndexOf("/"))
         val src ="/opt/static"+fields._1

         Thumbnails.of(src).sourceRegion(fields._2,fields._3,(fields._4-fields._2),(fields._5-fields._3)).size((fields._4-fields._2),(fields._5-fields._3)).toFile("/opt/static/images/site/"+picName)

        val picSrc:String= "/images/site/"+picName

        Ok(Json.obj("code"->"100","src"->picSrc))
      }
    )
  }

  /* 获得视频地址*/
def  getVideo = Users.UserAction {   user => implicit request =>

    Ok(Json.obj("code"->"100","src"->"sss"))
  }




}