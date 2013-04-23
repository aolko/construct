// Include StdAfx
#include "StdAfx.h"

//////////////////////////////////////////////////////////////////////////////////
// Built-in expressions
//////////////////////////////////////////////////////////////////////////////////

// Return the Default Value.  This is the value to be returned if your object name
// is used without an expression name, eg. in "Counter + 1".
// Parameters can be passed, eg. MyObject(3,7).  You can check ExpReturn::Type() or GetNumParams() to check for overloads.
long ExtObject::ReturnDefaultValue(LPVAL theParams, ExpReturn& ret)
{
	return ret = 0;
}

// Called for undefined expression names, if your extension allows it.
// Eg: MyObject.UndefinedExpression(3,4,5) calls this routine with
// "UndefinedExpression" as the expression name.  OF_UNDEFINEDEXPRESSIONS must be specified in Main.h.
long ExtObject::ReturnUndefinedExpression(CString& expName, LPVAL theParams, ExpReturn& ret)
{
	return ret = 0;
}

//////////////////////////////////////////////////////////////////////////////////
// Expressions
//////////////////////////////////////////////////////////////////////////////////

cr::point ExtObject::distortXY(LPVAL params)
{
	if(nodes.size() == 0)
		return cr::point(0,0);

	int row = params[1].GetInt();

	if(row < 0) row = 0;
	if(row > 1) row = 1;

	int col = params[0].GetInt();

	bool head = false;
	bool tail = false;
	if(col == columns)
		head = true;
	if(col == 0)
		tail = true;

	col = columns - col;
	col = col * step + step_offset;
	col = nodes.size() - col - 1;

	if(head)
		col = nodes.size() - 1;
	if(tail)
		col = 0;

	if(col < 0) col = 0;
	if(col >= nodes.size()) col = nodes.size() - 1;

	cr::point pos = nodes[col].pos + nodes[col].normal * width / 2;
	if(row == 1)
		pos = nodes[col].pos + nodes[col].normal * width / -2;

	return pos;
}


long ExtObject::eDistortX(LPVAL params, ExpReturn& ret)
{
	return ret = distortXY(params).x;
}

long ExtObject::eDistortY(LPVAL params, ExpReturn& ret)
{
	return ret = distortXY(params).y;
}

long ExtObject::eDistortU(LPVAL params, ExpReturn& ret)
{
	if(nodes.size() == 0)
		return ret = 0;

	int col = params[0].GetInt();

	if(col == columns)
		return ret = 0;
	if(col == 0)
		return ret = 0;

	return ret = -(step_offset / step) / columns;
}

long ExtObject::eDistortV(LPVAL params, ExpReturn& ret)
{
	return ret = 0;
}
