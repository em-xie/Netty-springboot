let sqlite = {
	//创建数据库或者有该数据库就打开
	openSqlite: function() {
		//创建数据库或者打开
		//这plus.sqlite只在手机上运行
		return new Promise((resolve, reject) => {
			console.log("打开数据库");
			plus.sqlite.openDatabase({
				name: "main", //数据库名称
				path: "_doc/sql.db", //数据库地址，uniapp推荐以下划线为开头，这到底存在哪里去了，我也不清楚，哈哈
				success(e) {
					console.log("success")
					resolve(e); //成功回调
				},
				fail(e) {
					console.log(e)
					reject(e); //失败回调
				},
			});
		});
	},

	getFriendMaxSeq: function(fromId, appId) {

		var sql = "";


		sql =
				"select ifnull(max(friend_sequence),0) as maxSeq from im_friendship " +
				" where app_id = " +
				 appId +
				" and from_id = '" +
				fromId + "'";
		return new Promise((resolve, reject) => {
			plus.sqlite.selectSql({
				name: "main",
				sql: sql,
				success(e) {
					console.log(e)
					resolve(e);
				},
				fail(e) {
					console.log(e)
					reject(e);
				},
			});
		});
	},

	addFriend: function(fromId, toId, remark, status, black, friendSeq, blackSeq, addSource, ex, appId) {
		var sql =
			" insert into im_friendship(from_id,to_id,remark,status,black,friend_sequence,black_sequence,add_source,extra,app_id) " +
			' values ("' +
			fromId +
			'","' +
			toId +
			'","' +
			remark +
			'","' +
			status +
			'","' +
			black +
			'","' +
			friendSeq +
			'","' +
			blackSeq +
			'","' +
			addSource +
			'","' +
			123 +
			'","' +
			appId +
			'")';
		console.log(sql)
		plus.sqlite.executeSql({
			name: "main",
			sql: sql,
			success(e) {
				console.log("插入成功")
				resolve(e);
			},
			fail(e) {
				console.log(e)
				reject(e);
			},
		});
	},

	bathAddFriend: function(data) {
		// console.log(data)
		var sql =
			" insert into im_friendship(from_id,to_id,remark,status,black,friend_sequence,black_sequence,add_source,extra,app_id) values ";
		for (var i = 0; i < data.length; i++) {
			var f = data[i];
			sql += ' ("' +
				f.fromId +
				'","' +
				f.toId +
				'","' +
				f.remark +
				'","' +
				f.status +
				'","' +
				f.black +
				'","' +
				f.friendSequence +
				'","' +
				f.blackSequence +
				'","' +
				f.addSource +
				'","' +
				123 +
				'","' +
				f.appId +
				'"),';
		}

		sql = sql.substring(0, sql.length - 1); //末尾是&
		// console.log(sql)
		plus.sqlite.executeSql({
			name: "main",
			sql: sql,
			success(e) {
				console.log("插入成功")
				resolve(e);
			},
			fail(e) {
				console.log(e)
				reject(e);
			},
		});
	},
	clearAllFriend: function() {
		var sql = "delete from im_friendship"
		plus.sqlite.executeSql({
			name: "main",
			sql: sql,
			success(e) {
				console.log("清除成功")
				resolve(e);
			},
			fail(e) {
				console.log(e)
				reject(e);
			},
		});
	},
	// ---------------------------------------------------监听数据库是否开启-----------------------------------------------------------------
	//监听数据库是否开启
	isOpen: function(name, path) {
		var ss = name || "main";
		var qq = path || "_doc/sql.db";
		//数据库打开了就返回true,否则返回false
		var open = plus.sqlite.isOpenDatabase({
			name: ss,
			path: qq,
		});
		return open;
	},

	// ---------------------------------------------------创建表----------------------------------------------------------------

	//在该数据库里创建表格， 这一步也必须要！
	//下面注释里说的都是说sql:'create table if not exists....这里
	//userInfo是表格名，你也可以写其他的名，不能用数字作为表格名的开头！！！ 
	//括号里是表格的结构，列，这里我写了四列，list,id,gender,avatar这四列
	//list后面大写的英文是自动增加的意思，因为表格里的每一行必须有唯一标识
	//这sql语句会数据库的应该都看的懂，我是前端菜鸡，所以详细说明以便跟我一样不懂sql的前端看
	//"id" TEXT 意思是这一列放的值为字符串之类的，如果是想存数字之类的就改为INTEGER
	//数据库不能存对象，数组
	//创建 9、定位表（d_location）
	createTable: function() {
		return new Promise((resolve, reject) => {
			//创建表格在executeSql方法里写
			plus.sqlite.executeSql({
				name: "main",
				//表格创建或者打开，后面为表格结构
				sql: "create table if not exists im_friendship(\"from_id\" text(50) NOT NULL,\"to_id\" text(50),\"app_id\" INTEGER(10) NOT NULL,\"remark\" TEXT(50),\"status\" int(5),\"black\" int(5),\"friend_sequence\" bigint(20),\"black_sequence\" bigint(20),\"add_source\" TEXT(20),\"extra\" TEXT(2000),PRIMARY KEY (\"app_id\", \"from_id\", \"to_id\"))",
				success(e) {
					console.log("创建成功")
					resolve(e);
				},
				fail(e) {
					console.log(e)
					console.log("创建失败")
					reject(e);
				},
			});
		});
	}
};

//把这些方法导出去
export default sqlite;
