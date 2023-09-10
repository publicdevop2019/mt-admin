import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-error-lookup',
  templateUrl: './error-lookup.component.html',
  styleUrls: ['./error-lookup.component.css']
})
export class ErrorLookupComponent implements OnInit {
  public data: {code:string,explain:string}[]=[
    {code:'1000',explain:'登录：密码错误'},
    {code:'1001',explain:'密码重置：找不到重置令牌'},
    {code:'1004',explain:'密码重置：重置令牌错误'},
    {code:'1005',explain:'Authorization找不到所请求客户端'},
    {code:'1006',explain:'Authorization:响应格式不支持'},
    {code:'1008',explain:'Authorization:找不到跳转URL'},
    {code:'1009',explain:'删除客户端：该客户端不允许删除'},
    {code:'1010',explain:'创建端口：Project Id不匹配'},
    {code:'1014,1015',explain:'找不到ProjectId'},
    {code:'1016,1029',explain:'没有该项目权限'},
    {code:'1017',explain:'读项目：没有该项目的读权限'},
    {code:'1018',explain:'端口报告查询：报告类型不支持'},
    {code:'1019',explain:'创建客户端：找不到客户端根角色（内部错误）'},
    {code:'1021',explain:'删除用户：系统默认用户无法被删除'},
    {code:'1024',explain:'租户用户注册：找不到项目默认用户角色'},
    {code:'1025',explain:'新用户注册：激活码错误'},
    {code:'1026',explain:'新用户注册：用户未点击发送注册码'},
    {code:'1030',explain:'权限：无该操作所需的权限'},
    {code:'1031',explain:'不是同一个用户创建'},
    {code:'1032',explain:'激活码为空'},
    {code:'1033',explain:'无法识别CacheControl值'},
    {code:'1034',explain:'客户端RoleId不可重写'},
    {code:'1035',explain:'客户端type不可重写'},
    {code:'1036',explain:'客户端type冲突'},
    {code:'1037',explain:'令牌刷新模式需开启密码登录'},
    {code:'1039',explain:'Origin格式错误'},
    {code:'1040',explain:'共享端口未过期前无法删除'},
    {code:'1041',explain:'过期端口为只读模式'},
    {code:'1042',explain:'端口仅已过期'},
    {code:'1043',explain:'内部端口无需过期'},
    {code:'1044',explain:'非共享端口无需过期'},
    {code:'1045',explain:'上传头像：保存图片错误'},
    {code:'1046',explain:'上传头像：图片格式错误'},
    {code:'1047',explain:'上传头像：图片过大'},
    {code:'1048',explain:'频繁操作，请等待'},
    {code:'1049,1051,1054',explain:'该对象不允许更改名称'},
    {code:'1050,1055',explain:'系统创建权限不允许更改'},
    {code:'1056',explain:'无法删除系统创建角色'},
    {code:'1057',explain:'不允许订阅自己的端口'},
    {code:'1058',explain:'不允许订阅过期端口'},
    {code:'1059',explain:'不允许订阅非共享端口'},
    {code:'1060,1061',explain:'订阅请求查询：type值错误'},
    {code:'1062',explain:'系统账户无法锁定'},
    {code:'1063',explain:'用户名无法重写'},
    {code:'1064',explain:'密码验证：至少一个字符'},
    {code:'1065',explain:'密码验证：至少一个数字'},
    {code:'1066',explain:'密码验证：不允许空格'},
    {code:'1067',explain:'密码验证：至少一个特殊字符'},
    {code:'1068',explain:'无法获得MD5校验值'},
    {code:'1070',explain:'登录：客户端密码错误'},
    {code:'1071',explain:'无法发送email'},
    {code:'1072',explain:'无法识别的query值'},
    {code:'1073',explain:'无法识别的order by值'},
    {code:'1074',explain:'无法识别的boolean值'},
    {code:'1077',explain:'该项目禁止此改动'},
    {code:'1078',explain:'项目找不到该用户'},
    {code:'1079',explain:'不可以对自己的权限进行操作'},
    {code:'1080',explain:'已经是管理员'},
    {code:'1081',explain:'不是管理员'},
    {code:'1082',explain:'至少两个管理员'},
    {code:'1083',explain:'当前用户ID提取错误'},
    {code:'1084',explain:'path格式错误'},
    {code:'1085',explain:'header格式错误'},
    {code:'1086',explain:'endpoint path格式错误'},
    {code:'1087',explain:'角色应属于同一项目'},
    {code:'1088',explain:'电话号码格式错误'},
    {code:'1089',explain:'Token参数错误'},
    {code:'1090',explain:'Refresh Token已过期'},
    {code:'1091',explain:'找不到客户端'},
    {code:'1092',explain:'endpoint重复'},
  ]
  public dataCommon: {code:string,explain:string}[]=[
    {code:'0000,0002,0003,0004,0006',explain:'MQ连接失败'},
    {code:'0001',explain:'MQ释放资源失败'},
    {code:'0005',explain:'消息消费失败'},
    {code:'0007',explain:'消息投递失败'},
    {code:'0009',explain:'聚合版本号过期'},
    {code:'0010,0011',explain:'DomainId不能为空'},
    {code:'0012',explain:'DomainId前缀错误'},
    {code:'0013',explain:'DomainId格式错误'},
    {code:'0014,0015',explain:'无法解析JWT'},
    {code:'0016,0017,0018,0019',explain:'无法获得JWT'},
    {code:'0020',explain:'不支持回滚'},
    {code:'0021',explain:'无法解析分页信息'},
    {code:'0026',explain:'超过最大允许分页大小'},
    {code:'0022',explain:'无法解析Long'},
    {code:'0023',explain:'无法解析Int'},
    {code:'0024',explain:'找不到分页，排序信息'},
    {code:'0025',explain:'超过最大每页数据大小'},
    {code:'0027',explain:'无法解析查询信息'},
    {code:'0028',explain:'非法查询字段'},
    {code:'0029',explain:'null值不允许'},
    {code:'0030',explain:'非法查询值'},
    {code:'0031',explain:'无更新字段'},
    {code:'0032',explain:'不支持该Patch改动'},
    {code:'0033',explain:'该Patch改动返回值不为1'},
    {code:'0034,0061',explain:'InstanceId错误'},
    {code:'0035,0062',explain:'时钟回调'},
    {code:'0036',explain:'校验：不能为空白'},
    {code:'0037',explain:'校验：不能为null'},
    {code:'0038',explain:'校验：过短'},
    {code:'0039',explain:'校验：过长'},
    {code:'0040',explain:'校验：非法字符'},
    {code:'0041',explain:'校验：不能为空'},
    {code:'0042',explain:'校验：元素不能为null'},
    {code:'0044',explain:'校验：数值过小或不等'},
    {code:'0045',explain:'校验：不等'},
    {code:'0046',explain:'校验：数值过小'},
    {code:'0047',explain:'校验：不是Email'},
    {code:'0048',explain:'校验：不是URL'},
    {code:'0049',explain:'Http Validation'},
    {code:'0050,0051,0052,0053,0054,0055,0056',explain:'反/序列化错误'},
    {code:'0057',explain:'JSON patch失败'},
    {code:'0058',explain:'错误的枚举值'},
    {code:'0059',explain:'值必须为空'},
    {code:'0061',explain:'实际值与期待值校验失败'},
    {code:'0062',explain:'校验：不是空集合'},
    {code:'0063',explain:'请开启事务'},
    {code:'0064',explain:'数据库更新失败'},
  ]
  constructor() { }

  ngOnInit(): void {
  }

}
