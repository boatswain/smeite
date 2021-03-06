package controllers

import play.api.mvc.{Action, Controller}
import controllers.users.Users

import play.api.data.Forms._
import play.api.data.Form
import play.api.libs.json._
import models.forum._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Whitelist
import models.user.dao.UserDao
import  models.user._
import models.forum.dao.TopicDao
import models.advert.dao.AdvertDao
import scala.Some
import models.forum.Topic
import models.forum.TopicReply
import play.api.libs.functional.syntax._
import scala.Some
import models.forum.Topic
import models.forum.TopicReply
import models.goods.dao.GoodsDao
import play.api.mvc.BodyParsers.parse


/**
 * Created by zuosanshao.
 * User: zuosanshao
 * Date: 12-10-23
 * Time: 下午8:04
 * Email:zuosanshao@qq.com
 */
/* smeite.com 编辑器 所用的 editor goods decode  */
case class EditorGoods(
                           goodsId:Long,
                           numIid:Long,
                           name:String,
                           intro:String,
                           price:String,
                           pic:String,
                           loveNum:Int,
                           volume:Int,
                           location:String
                           )

object Forums extends Controller {
  val topicForm = Form(
    tuple(
       "id"->optional(longNumber),
      "title" ->nonEmptyText ,
      "groupId"->number,
      "typeId" ->number,
      "content"->nonEmptyText
    )
  )
  val replyForm =Form(
  tuple(
   "topicId"->number,
   "content"->nonEmptyText,
    "replyContent"->optional(text)
  )
  )
  var searchForm =Form(
  tuple(
  "groupId"->number, 
  "typeId"->number,
  "text"->text
  )
  )


  implicit val editorGoodsFormat = (
    (__ \ "goodsId").format[Long] and
      (__ \ "numIid").format[Long] and
      (__ \ "name").format[String] and
      (__ \ "intro").format[String] and
      (__ \ "price").format[String] and
      (__ \ "pic").format[String] and
      (__ \ "loveNum").format[Int] and
      (__ \ "volume").format[Int] and
      (__ \ "location").format[String]
    )(EditorGoods.apply,unlift(EditorGoods.unapply))

  /*讨论吧 */
  def forum(p:Int) =Users.UserAction { user => implicit request =>
    val page =TopicDao.findTopics(0,p,10)
    val wenTopics=AdvertDao.getTopics("hot_question")
    val zhiTopics=AdvertDao.getTopics("hot_knowledge")
    val users=UserDao.recommendUser(0,10)
    val advert =AdvertDao.findAdvert("forum_activity");
    Ok(views.html.forums.forum(user,page,wenTopics,zhiTopics,users,advert))
  }


  /*create 帖子 */
  def edit(id:Long) = Users.UserAction { user => implicit request =>
  //  if (user.isEmpty){
  //    Redirect(controllers.users.routes.UsersRegLogin.login)
 //   } else{
      if(id ==0)Ok(views.html.forums.edit(user,topicForm))
      else {
        val topic=TopicDao.findById(id);
        if (topic.isEmpty)Ok(views.html.forums.edit(user,topicForm))
        else Ok(views.html.forums.edit(user,topicForm.fill((topic.get.id,topic.get.title,topic.get.groupId,topic.get.typeId,topic.get.content))))
      }
//    }

  }

   /*保存 帖子*/
  def save =Users.UserAction{user => implicit request =>
    topicForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.forums.edit(user,formWithErrors)),
      fields =>{
        var intro =Jsoup.clean(fields._5,Whitelist.none())
        if(intro.length()>100) intro =intro.substring(0,100)+"..."
       val  doc =Jsoup.parseBodyFragment(fields._5)
        val goodsImages =doc.body().getElementsByClass("img-goods")
       val uploadImages= doc.body().getElementsByClass("img-upload")
       var pics =""
        val it=uploadImages.iterator()
        while(it.hasNext){
          pics +=it.next().attr("src")+","
        }
        val it2= goodsImages.iterator()
        while(it2.hasNext){
          pics +=it2.next().attr("src")+","
        }
        if (fields._1.isEmpty){
          val id= TopicDao.addTopic(Topic(None,user.get.id.get,user.get.name,fields._2,fields._5,intro,Some(pics.trim),fields._3,fields._4,false,false,0,0,1,0,1,None))
          Redirect(controllers.routes.Forums.view(id))
        }else{
           TopicDao.modifyTopic(Topic(fields._1,user.get.id.get,user.get.name,fields._2,fields._5,intro,Some(pics.trim),fields._3,fields._4,false,false,0,0,1,0,1,None))
          Redirect(controllers.routes.Forums.view(fields._1.get))
        }


      }
    )

  }
  /*帖子详情 只有存在且checkstate=1 或者 此贴子是作者发的 才可以显示*/
  def view(id:Long,p:Int) = Users.UserAction { user => implicit request =>
    val hotIndex=0
     val topic=TopicDao.findByIdWithUser(id)
     if((!topic.isEmpty && topic.get._2.checkState==1) || (!user.isEmpty && user.get.id.get==topic.get._2.uid)){
       val page=TopicDao.findTopicReplies(id,p,10);
       val topics=TopicDao.recommendTopics(8);// 推荐最新话题
       Ok(views.html.forums.view(user,topic.get,page,topics))
     } else Ok(views.html.forums.empty())
    
  }

def addComment =Action(parse.json){  implicit request =>
  val user:Option[User] =request.session.get("user").map(u=>UserDao.findById(u.toLong))
  if(user.isEmpty)Ok(Json.obj("code" -> "200", "message" ->"亲，你还没有登录哦" ))
  else if(user.get.status==4)Ok(Json.obj("code" -> "444", "message" -> "亲，你违反了社区规定，目前禁止评论"))
  else {
    val topicId = (request.body \ "topicId").asOpt[Long]
    val  content =(request.body \ "content").asOpt[String]
    val  quoteContent=(request.body \ "quoteContent").asOpt[String]
    if(content.isEmpty || topicId.isEmpty ){
      Ok(Json.obj("code" -> "101", "message" ->"亲，是不是没有输入内容？请重新提交试试"))
    }else{
      TopicDao.addReply(user.get.id.get,user.get.name,topicId.get,quoteContent,content.get,1)
      Ok(Json.obj("code" -> "100", "message" ->"亲，评论成功"))
    }
  }

}

  def checkTopicLoveState = Action(parse.json){  implicit request =>
    val user:Option[User] =request.session.get("user").map(u=> UserDao.findById(u.toLong) )
    if(user.isEmpty)  Ok(Json.obj("code" -> "300","message" -> "亲，你还没有登录呢" ))
    else{
      val topicId=(request.body \ "topicId").asOpt[Long];
      if (topicId.isEmpty)Ok(Json.obj("code"->"104","message"->"param id is empty"))
      else{
        val follow =UserDao.checkLoveTopic(user.get.id.get,topicId.get);
        if(!follow.isEmpty)  Ok(Json.obj("code" -> "100","message" -> "已关注" ))
        else Ok(Json.obj("code" -> "101","message" -> "未关注" ))
      }

    }
  }

  def addFollow = Action(parse.json){  implicit request =>
    val user:Option[User] =request.session.get("user").map(u=> UserDao.findById(u.toLong) )
    if(user.isEmpty)  Ok(Json.obj("code" -> "300","message" -> "亲，你还没有登录呢" ))
    else if(user.get.status ==4) Ok(Json.obj("code" -> "444","message" -> "亲，你被禁止了" ))
    else {
      val topicId=(request.body \ "topicId").asOpt[Long]
      if (topicId.isEmpty)Ok(Json.obj("code"->"104","message"->"param id is empty"))
      else {
      val follow =UserDao.checkLoveTopic(user.get.id.get,topicId.get);
      if(!follow.isEmpty) Ok(Json.obj("code" -> "103","message" ->"重复关注了"))
      else {
        UserDao.addLoveTopic(user.get.id.get,topicId.get)
       Ok(Json.obj("code" -> "100","message" -> "关注成功" ))
      }
    }
    }

  }
  /* 取消关注 code 100 成功  104 参数错误 300 用户未登录*/
  def removeFollow =  Action(parse.json){  implicit request =>
    val user:Option[User] =request.session.get("user").map(u=> UserDao.findById(u.toLong) )
    if(user.isEmpty)  Ok(Json.obj("code" -> "300", "message" ->"亲，你还没有登录呢"  ))
    else{
      val topicId=(request.body \ "topicId").asOpt[Long];
      if (topicId.isEmpty)Ok(Json.obj("code"->"104","message"->"param id is empty"))
      else{
        UserDao.deleteLoveTopic(user.get.id.get,topicId.get)
        Ok(Json.obj("code" -> "100","message" -> "取消成功" ))
      }

    }
  }


  /*
  *
  * 获取帖子中的宝贝
  *
  * */
 def fetchBaobei(id:Long) = Users.UserAction{ user => implicit request =>
      val goods = GoodsDao.findById(id)
    if(goods.isEmpty){
      Ok(Json.obj("code"->104,"msg" -> "宝贝不存在" ))
    }else {


      val baobei = EditorGoods(goods.get.id.get,goods.get.numIid,goods.get.name,goods.get.intro,goods.get.price,goods.get.pic,goods.get.loveNum,goods.get.volume,goods.get.location)
      Ok(Json.obj("code"->100,"baobei"->Json.toJson(baobei),"msg"->"success" ))
    }

  }


}
