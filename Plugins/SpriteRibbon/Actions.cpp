// Include StdAfx
#include "StdAfx.h"

//////////////////////////////////////////////////////////////////////////////////
// Actions
//////////////////////////////////////////////////////////////////////////////////

long ExtObject::aAddPoint(LPVAL params)
{
	RibbonNode node;
	node.pos.x = params[0].GetFloat();
	node.pos.y = params[1].GetFloat();

	nodes.push_back(node);

	while (nodes.size() > maxPoints)
		nodes.erase(nodes.begin());
	
	createRibbonNormals();

	step_offset ++;
	while(step_offset > step)
		step_offset -= step;

	return 0;
}

long ExtObject::aSetNumberOfPoints(LPVAL params)
{
	maxPoints = max(1, params[0].GetInt() );
	return 0;
}

long ExtObject::aSetThickness(LPVAL params)
{
	width = params[0].GetFloat();
	return 0;
}

long ExtObject::aSetSegments(LPVAL params)
{
	columns = max(1, params[0].GetInt() );
	return 0;
}
