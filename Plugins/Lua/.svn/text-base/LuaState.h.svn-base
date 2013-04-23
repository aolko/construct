//////////////////////////////////////////////////////////////////////////
///@author John Hannagan
/// 
///@brief	This is a simple high level wrapper class for the c style 
///			lua_state object. It encapsulates the lua state at a higher level.
//////////////////////////////////////////////////////////////////////////

#ifndef LUA_STATE
#define LUA_STATE

#include "luabind/luabind.hpp"

extern "C" {
#include "lua/lua.h"
#include "lua/lualib.h"
#include "lua/lauxlib.h"
}


#include <iostream>
 

class LuaState
{
public:
	LuaState();
	~LuaState();

	lua_State* getState();

	//Generic lua c api wrapper functions
	void dostring( const std::string& str   );
	void dofile(   const std::string& fname );

	template<class T>
	void addGlobal(const std::string& globalName, T* ptr );

	///Gets the value of a global from the global lua table and
	///attempts to convert it to an integer
	///@param globalName the name of the variable to get
	int		getGlobalInt(	const std::string& globalName );
	///Gets the value of a global from the global lua table and
	///attempts to convert it to a float
	///@param globalName the name of the variable to get
	float	getGlobalFloat( const std::string& globalName );
	///Gets the value of a global from the global lua table and
	///attempts to convert it to a bool
	///@param globalName the name of the variable to get
	bool	getGlobalBool( const std::string& globalName );
	///Gets the value of a global from the global lua table and
	///attempts to convert it to a string
	///@param globalName the name of the variable to get
	std::string	getGlobalString( const std::string& globalName );

private:
	void printErrors( int status );

	///This function binds all the C++ classes and functions to lua
	void initLuaBind();

	lua_State* L;
};



//------------------------------------------------------------------------
//  Template functions
//------------------------------------------------------------------------

template<class T>
void LuaState::addGlobal(const std::string& globalName, T* ptr )
{
	luabind::globals(L)[globalName] = ptr;
}


#endif