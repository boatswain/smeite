/**
 * Created by zuosanshao.
 * User: Administrator
 * Date: 12-10-2
 * Time: 下午6:52
 * Email:zuosanshao@qq.com
 */
define(function (require,exports) {
    var $  = require("$");
    require("smeite/common/validator")($);
    jQuery(function() {
        $("#J_regLoginForm").validator();
    });
});