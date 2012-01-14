/*
 *  Dracula Graph Layout and Drawing Framework 0.0.3alpha
 *  (c) 2010 Philipp Strathausen <strathausen@gmail.com>, http://strathausen.eu
 *  Contributions by Jake Stothard <stothardj@gmail.com>.
 *
 *  based on the Graph JavaScript framework, version 0.0.1
 *  (c) 2006 Aslak Hellesoy <aslak.hellesoy@gmail.com>
 *  (c) 2006 Dave Hoover <dave.hoover@gmail.com>
 *
 *  Ported from Graph::Layouter::Spring in
 *    http://search.cpan.org/~pasky/Graph-Layderer-0.02/
 *  The algorithm is based on a spring-style layouter of a Java-based social
 *  network tracker PieSpy written by Paul Mutton <paul@jibble.org>.
 *
 *  This code is freely distributable under the MIT license. Commercial use is
 *  hereby granted without any cost or restriction.
 *
 *  Links:
 *
 *  Graph Dracula JavaScript Framework:
 *      http://graphdracula.net
 *  
 *  ///////////////////////////////////
 *  
 *  IMPORTANT NOTE:
 *  	This file has contains code modifications, which may need to be
 *  	retained when updating the library.  The modified sections of code
 *  	are clearly marked in a style similar to this comment.
 *  
 *  ///////////////////////////////////
 *
 /*--------------------------------------------------------------------------*/

/* Padding for self-loops to not display outside canvas bounds. */
var SelfLoopPadding = 60;

/*
 * Edge Factory
 */
var AbstractEdge = function() {
}
AbstractEdge.prototype = {
    hide: function() {
        this.connection.fg.hide();
        this.connection.bg && this.bg.connection.hide();
    }
};
var EdgeFactory = function() {
    this.template = new AbstractEdge();
    this.template.style = new Object();
    this.template.style.directed = false;
    this.template.weight = 1;
};
EdgeFactory.prototype = {
    build: function(source, target) {
        var e = jQuery.extend(true, {}, this.template);
        e.source = source;
        e.target = target;
        return e;
    }
};

/*
 * Graph
 */
var Graph = function() {
    this.nodes = {};
    this.edges = [];
    this.snapshots = []; // previous graph states TODO to be implemented
    this.edgeFactory = new EdgeFactory();
};
Graph.prototype = {
    /*
     * add a node
     * @id          the node's ID (string or number)
     * @content     (optional, dictionary) can contain any information that is
     *              being interpreted by the layout algorithm or the graph
     *              representation
     */
    addNode: function(id, content) {
        /* testing if node is already existing in the graph */
        if(this.nodes[id] == undefined) {
            this.nodes[id] = new Graph.Node(id, content);
        }
        return this.nodes[id];
    },

    addEdge: function(source, target, style) {
        var s = this.addNode(source);
        var t = this.addNode(target);
        var edge = this.edgeFactory.build(s, t);
        jQuery.extend(edge.style,style);
        s.edges.push(edge);
        this.edges.push(edge);
        // NOTE: Even directed edges are added to both nodes.
        t.edges.push(edge);
    },

    /* TODO to be implemented
     * Preserve a copy of the graph state (nodes, positions, ...)
     * @comment     a comment describing the state
     */
    snapShot: function(comment) {
        /* FIXME
        var graph = new Graph();
        graph.nodes = jQuery.extend(true, {}, this.nodes);
        graph.edges = jQuery.extend(true, {}, this.edges);
        this.snapshots.push({comment: comment, graph: graph});
        */
    },
    removeNode: function(id) {
    	var node = this.nodes[id];
    	node.hidden = true;
        node.shape && node.shape.hide(); /* FIXME this is representation specific code and should be elsewhere */
        delete this.nodes[id];
        for(var i = 0; i < this.edges.length; i++) {
            if (this.edges[i].source.id == id || this.edges[i].target.id == id) {
            	
            	
            	/*////////////////////////////////////////////////////
                 * 
                 * THIS CODE IS NOT ORIGINALLY PART OF THE LIBRARY.
                 * if the library is updated, changes here might be lost.
                 * 
                 *////////////////////////////////////////////////////
            	
            	// Hide the label (TODO: put this somewhere more appropriate, like 
            	// in the hide function for the edge itself).
            	this.edges[i].connection && this.edges[i].connection.label.hide();
            	
            	/*///////////////////////////////////////////////////////
            	 * 
            	 * END OF MODIFIED CODE
            	 * 
            	 *///////////////////////////////////////////////////////
            	this.edges[i].hide();
                this.edges.splice(i, 1);
                i--;
            }
        }
    }
};

/*
 * Node
 */
Graph.Node = function(id, node){
    node = node || {};
    node.id = id;
    node.edges = [];
    node.hide = function() {
        this.hidden = true;
        this.shape && this.shape.hide(); /* FIXME this is representation specific code and should be elsewhere */
        for(i in this.edges)
            (this.edges[i].source.id == id || this.edges[i].target == id) && this.edges[i].hide && this.edges[i].hide();
    };
    node.show = function() {
        this.hidden = false;
        this.shape && this.shape.show();
        for(i in this.edges)
            (this.edges[i].source.id == id || this.edges[i].target == id) && this.edges[i].show && this.edges[i].show();
    };
    return node;
};
Graph.Node.prototype = {
};

/*
 * Renderer base class
 */
Graph.Renderer = {};

/*
 * Renderer implementation using RaphaelJS
 */
Graph.Renderer.Raphael = function(element, graph, width, height) {
    this.width = width || 400;
    this.height = height || 400;
    var selfRef = this;
    this.r = Raphael(element, this.width, this.height);
    this.radius = 40; /* max dimension of a node */
    this.graph = graph;
    this.mouse_in = false;
    this.animationDuration = 1500;

    /* TODO default node rendering function */
    if(!this.graph.render) {
        this.graph.render = function() {
            return;
        }
    }

    /*
     * Dragging
     */
    this.isDrag = false;
    this.dragger = function (e) {
        this.dx = e.clientX;
        this.dy = e.clientY;
        selfRef.isDrag = this;
        this.set && this.set.animate({"fill-opacity": .1}, 200) && this.set.toFront();
        e.preventDefault && e.preventDefault();
    };

    var d = document.getElementById(element);
    d.onmousemove = function (e) {
        e = e || window.event;
        if (selfRef.isDrag) {
            var bBox = selfRef.isDrag.set.getBBox();
            // TODO round the coordinates here (eg. for proper image representation)
            var newX = e.clientX - selfRef.isDrag.dx + (bBox.x + bBox.width / 2);
            var newY = e.clientY - selfRef.isDrag.dy + (bBox.y + bBox.height / 2);
            
            /* making changes external changes to Dracula code to fix issue 
             * regarding nodes draggable outside the canvas space */
            /* NOTE: offX and offY values were previously hard-coded to 20
            /* center of shape is considered in layout, so must account for one
             * half of the shape to not be drawn outside canvas */
            var offX = bBox.width / 2;
            var offY = bBox.height / 2;
            
            /* prevent shapes from being dragged out of the canvas */
            var clientX = e.clientX - (newX < offX ? newX - offX : newX > selfRef.width - offX ? newX - selfRef.width + offX : 0);
            var clientY = e.clientY - (newY < offY ? newY - offY : newY > selfRef.height - offY ? newY - selfRef.height + offY : 0);
            selfRef.isDrag.set.translate(clientX - Math.round(selfRef.isDrag.dx), clientY - Math.round(selfRef.isDrag.dy));
            //            console.log(clientX - Math.round(selfRef.isDrag.dx), clientY - Math.round(selfRef.isDrag.dy));
            for (var i in selfRef.graph.edges) {
                selfRef.graph.edges[i].connection && selfRef.graph.edges[i].connection.draw();
            }
            //selfRef.r.safari();
            selfRef.isDrag.dx = clientX;
            selfRef.isDrag.dy = clientY;
        }
    };
    d.onmouseup = function () {
        selfRef.isDrag && selfRef.isDrag.set.animate({"fill-opacity": .6}, 500);
        selfRef.isDrag = false;
    };
    this.draw();
};
Graph.Renderer.Raphael.prototype = {

	translate: function(point) {
        return [
            (point[0] - this.graph.layoutMinX) * this.factorX + this.radius,
            (point[1] - this.graph.layoutMinY) * this.factorY + this.radius
        ];
    },

    rotate: function(point, length, angle) {
        var dx = length * Math.cos(angle);
        var dy = length * Math.sin(angle);
        return [point[0]+dx, point[1]+dy];
    },

    draw: function() {
        this.factorX = (this.width - 2 * this.radius) / (this.graph.layoutMaxX - this.graph.layoutMinX);
        this.factorY = (this.height - 2 * this.radius) / (this.graph.layoutMaxY - this.graph.layoutMinY);
        for (i in this.graph.nodes) {
            this.drawNode(this.graph.nodes[i]);
        }
        for (var i = 0; i < this.graph.edges.length; i++) {
            this.drawEdge(this.graph.edges[i]);
        }
    },

    drawNode: function(node) {

        var point = this.translate([node.layoutPosX, node.layoutPosY]);
        node.point = point;

        /* if node has already been drawn, move the nodes */
        if(node.shape) {
        	// to move edges along with nodes
        	var updateEdges = function (graph, count, animationDuration) {
        		for (var i in graph.edges) {
                    graph.edges[i].connection && graph.edges[i].connection.draw();
                }
        		if (count < animationDuration) {
        			setTimeout(updateEdges, 10, graph, count + 50, animationDuration);
        		}
        	}
        	// new destination
        	var shape = node.render(this.r, node).hide();
        	var box = shape.getBBox();
        	        	
        	var xtrans = Math.round(point[0]-(box.x+box.width/2));
        	var ytrans = Math.round(point[1]-(box.y+box.height/2));
        	
        	shape.translate(xtrans, ytrans);
        	
        	var xnew = box.x + xtrans;
        	var ynew = box.y + ytrans;
        	
        	/* Calling a function that is not a part of original library */
            translateInsideCanvas(shape, box, xnew, ynew, this.width, this.height);
        	
        	//shape.translate(Math.round(point[0]-(box.x+box.width/2)),Math.round(point[1]-(box.y+box.height/2)));

        	// animate all items in this node's set
        	for (var i = 0; i < node.shape.items.length; i++) {
        		node.shape.items[i].animate({x :shape.items[i].attr("x")}, this.animationDuration, '<>');
        		node.shape.items[i].animate({y :shape.items[i].attr("y")}, this.animationDuration, '<>');
        	}

        	// set timer to animate edges
        	setTimeout(updateEdges, 10, this.graph, 0, this.animationDuration);

            return node;
        	/*
        	 * original behavior:
        	 * var oBBox = node.shape.getBBox();
             * var opoint = { x: oBBox.x + oBBox.width / 2, y: oBBox.y + oBBox.height / 2};
             * node.shape.translate(Math.round(point[0] - opoint.x), Math.round(point[1] - opoint.y));
             * this.r.safari();
             * return node;
             */

        }/* else, draw new nodes */

        var shape;

        /* if a node renderer function is provided by the user, then use it
           or the default render function instead */
        if(!node.render) {
            node.render = function(r, node) {
                /* the default node drawing */
                var color = Raphael.getColor();
                var ellipse = r.ellipse(0, 0, 30, 20).attr({fill: color, stroke: color, "stroke-width": 2});
                /* set DOM node ID */
                ellipse.node.id = node.label || node.id;
                shape = r.set().
                    push(ellipse).
                    push(r.text(0, 30, node.label || node.id));
                return shape;
            }
        }
        /* or check for an ajax representation of the nodes */
        if(node.shapes) {
            // TODO ajax representation evaluation
        }

        shape = node.render(this.r, node).hide();

        shape.attr({"fill-opacity": .6 });
        /* re-reference to the node an element belongs to, needed for dragging all elements of a node */
        shape.items.forEach(function(item){ item.set = shape; item.node.style.cursor = "move"; });
        shape.mousedown(this.dragger);

        var box = shape.getBBox();
              
        var xtrans = Math.round(point[0]-(box.x+box.width/2));
        var ytrans = Math.round(point[1]-(box.y+box.height/2));
        
        shape.translate(xtrans, ytrans);
        
        var xnew = box.x + xtrans;
        var ynew = box.y + ytrans;
        
        /* Calling a function that is not a part of original library */
        translateInsideCanvas(shape, box, xnew, ynew, this.width, this.height);
        
        //console.log(box,point);
        node.hidden || shape.show();
        node.shape = shape;
    },

    drawEdge: function(edge) {
        /* if this edge already exists the other way around and is undirected */
        if(edge.backedge)
            return;
        if(edge.source.hidden || edge.target.hidden) {
            edge.connection && edge.connection.fg.hide() | edge.connection.bg && edge.connection.bg.hide();
            return;
        }

        /* if edge already has been drawn, only refresh the edge */
        if(!edge.connection) {
            edge.style && edge.style.callback && edge.style.callback(edge); // TODO move this somewhere else
            edge.connection = this.r.connection(edge.source.shape, edge.target.shape, edge.style);
            return;
        }
        //FIXME showing doesn't work well
        edge.connection.fg.show();
        edge.connection.bg && edge.connection.bg.show();
        edge.connection.draw();

    }
};
Graph.Layout = {};
Graph.Layout.Spring = function(graph) {
    this.graph = graph;
    this.iterations = 500;
    this.maxRepulsiveForceDistance = 6;
    this.k = 2;
    this.c = 0.01;
    this.maxVertexMovement = 0.5;
    this.layout();
};
Graph.Layout.Spring.prototype = {
    layout: function() {
        this.layoutPrepare();
        for (var i = 0; i < this.iterations; i++) {
            this.layoutIteration();
        }
        this.layoutCalcBounds();
    },

    layoutPrepare: function() {
        for (i in this.graph.nodes) {
            var node = this.graph.nodes[i];
            node.layoutPosX = 0;
            node.layoutPosY = 0;
            node.layoutForceX = 0;
            node.layoutForceY = 0;
        }

    },

    layoutCalcBounds: function() {
        var minx = Infinity, maxx = -Infinity, miny = Infinity, maxy = -Infinity;

        for (i in this.graph.nodes) {
             var x = this.graph.nodes[i].layoutPosX;
             var y = this.graph.nodes[i].layoutPosY;

            /*////////////////////////////////////////////////////
             * 
             * THIS CODE IS NOT ORIGINALLY PART OF THE LIBRARY.
             * if the library is updated, changes here might be lost.
             * 
             *////////////////////////////////////////////////////
             
             // Keeps nodes away from the edges.
            if(x > maxx) maxx = x + 0.6;
            if(x < minx) minx = x - 0.6;
            if(y > maxy) maxy = y + 0.6;
            if(y < miny) miny = y - 0.6;
            
            /*///////////////////////////////////////////////////////
             * 
             * END OF MODIFICATION.
             * 
             *///////////////////////////////////////////////////////
        }
        
        this.graph.layoutMinX = minx;
        this.graph.layoutMaxX = maxx;
        this.graph.layoutMinY = miny;
        this.graph.layoutMaxY = maxy;
    },

    layoutIteration: function() {
    	this.calculateForces();
        // Move by the given force
        for (i in this.graph.nodes) {
            var node = this.graph.nodes[i];
            this.updateNode(node);
        }
    },

    layoutRepulsive: function(node1, node2) {
        var dx = node2.layoutPosX - node1.layoutPosX;
        var dy = node2.layoutPosY - node1.layoutPosY;
        var d2 = dx * dx + dy * dy;
        if(d2 < 0.01) {
            dx = 0.1 * Math.random() + 0.1;
            dy = 0.1 * Math.random() + 0.1;
            var d2 = dx * dx + dy * dy;
        }
        var d = Math.sqrt(d2);
        if(d < this.maxRepulsiveForceDistance) {
            var repulsiveForce = this.k * this.k / d;
            node2.layoutForceX += repulsiveForce * dx / d;
            node2.layoutForceY += repulsiveForce * dy / d;
            node1.layoutForceX -= repulsiveForce * dx / d;
            node1.layoutForceY -= repulsiveForce * dy / d;
        }
    },

    layoutAttractive: function(edge) {
        var node1 = edge.source;
        var node2 = edge.target;

        var dx = node2.layoutPosX - node1.layoutPosX;
        var dy = node2.layoutPosY - node1.layoutPosY;
        var d2 = dx * dx + dy * dy;
        if(d2 < 0.01) {
            dx = 0.1 * Math.random() + 0.1;
            dy = 0.1 * Math.random() + 0.1;
            var d2 = dx * dx + dy * dy;
        }
        var d = Math.sqrt(d2);
        if(d > this.maxRepulsiveForceDistance) {
            d = this.maxRepulsiveForceDistance;
            d2 = d * d;
        }
        var attractiveForce = (d2 - this.k * this.k) / this.k;
        if(edge.attraction == undefined) edge.attraction = 1;
        attractiveForce *= Math.log(edge.attraction) * 0.5 + 1;

        node2.layoutForceX -= attractiveForce * dx / d;
        node2.layoutForceY -= attractiveForce * dy / d;
        node1.layoutForceX += attractiveForce * dx / d;
        node1.layoutForceY += attractiveForce * dy / d;
    },

    // moves the given node using its current forces
    updateNode: function(node) {
        var xmove = this.c * node.layoutForceX;
        var ymove = this.c * node.layoutForceY;

        var max = this.maxVertexMovement;
        if(xmove > max) xmove = max;
        if(xmove < -max) xmove = -max;
        if(ymove > max) ymove = max;
        if(ymove < -max) ymove = -max;

        node.layoutPosX += xmove;
        node.layoutPosY += ymove;
        node.layoutForceX = 0;
        node.layoutForceY = 0;
    },

    // calculates repulsive and attractive forces on each node in the graph.
    // forces are stored in each node with layoutForceX and layoutForceY
    // fields. the layoutRepulsive and layoutAttractive methods modify those fields.
    calculateForces: function() {
        // Forces on nodes due to node-node repulsions
        var prev = new Array();
        for(var c in this.graph.nodes) {
            var node1 = this.graph.nodes[c];
            for (var d in prev) {
                var node2 = this.graph.nodes[prev[d]];
                this.layoutRepulsive(node1, node2);

            }
            prev.push(c);
        }

        // Forces on nodes due to edge attractions
        for (var i = 0; i < this.graph.edges.length; i++) {
            var edge = this.graph.edges[i];
            this.layoutAttractive(edge);
        }
    }
};

// Provides stable positions for nodes with the given initial or terminal labels
Graph.Layout.Stable = function (graph, initial, terminal) {

	// internal springLayout used to calculate node positions
	this.springLayout = new Graph.Layout.Spring(graph);

    this.graph = graph;
    this.initial = initial;		// label on initial nodes
    this.terminal = terminal;	// label on terminal nodes

    this.updates = 50;     // number of times to re-calculate node positions
                           // by calculating forces when updateLayout is called

    // do initial layout
    this.layout();
};

// Stable Layout prototype that provides similar functionality to Spring Layout
// aside from special treatment of initial/terminal nodes, and an additional
// updateLayout() function for updating the positions of given new nodes
Graph.Layout.Stable.prototype = {

	// initial layout:
	//    layoutPrepare() assigns initial positions to each node
	//    layoutIteration() is call a set number of times (Spring Layout's iterations),
	//          and updates positions by calculating forces on nodes
	//    layoutCalcBounds() tells the graph the min and max positions of all nodes, used
	//          when rendering to ensure that the graph stretches to take all available space
    layout: function() {
        this.layoutPrepare();
        for (var i = 0; i < this.springLayout.iterations; i++) {
            this.layoutIteration();
        }
       this.springLayout.layoutCalcBounds();
	},

	// called on graphs that have already been laid out - updates the position
	// of nodes in the newNodes array by calculating forces against all other nodes
	// in the graph
	updateLayout : function(g, newNodes) {
		// reset initial forces on all nodes to 0
        for (i in this.graph.nodes) {
            var node = this.graph.nodes[i];
            node.layoutForceX = 0;
            node.layoutForceY = 0;
        }
        // updates the position of new nodes <updates> times
		for (var i = 0; i < this.updates; i++) {
			this.layoutUpdateIteration(newNodes);
		}
		// recalculates graph bounds
		this.springLayout.layoutCalcBounds();
	},

	// prepares nodes to be laid out by assigning default initial x, y positions
	// and default x, y forces. terminal nodes are given x positions at the
	// right side of the graph, all others assigned x positions on the left
    layoutPrepare: function() {
        for (i in this.graph.nodes) {
            var node = this.graph.nodes[i];
            if (node.label !== this.terminal) {
            	node.layoutPosX = 0;
            } else {
            	node.layoutPosX = 10;
            }
            node.layoutPosY = 0;
            node.layoutForceX = 0;
            node.layoutForceY = 0;
        }
    },

    // updates node positions of all nodes aside from the initial and terminal nodes
    // by re-calculating forces on all nodes and updating node positions
    layoutIteration: function() {
        this.springLayout.calculateForces();
        // Move by the given force
        for (i in this.graph.nodes) {
            var node = this.graph.nodes[i];
            if (node.label !== this.initial && node.label !== this.terminal) {
            	this.springLayout.updateNode(node);
            }
        }
    },

    // updates node positions of the given new nodes by re-calculating forces on all
    // nodes and using those forces to update the positions of new nodes
    layoutUpdateIteration: function(newNodes) {
    	this.springLayout.calculateForces();
        // Move by the given force
        for (i in this.graph.nodes) {
            var node = this.graph.nodes[i];
            if (newNodes[node.id]) {
            	this.springLayout.updateNode(node);
            }
        }
    }
};

Graph.Layout.Ordered = function(graph, order) {
    this.graph = graph;
    this.order = order;
    this.layout();
};
Graph.Layout.Ordered.prototype = {
    layout: function() {
        this.layoutPrepare();
        this.layoutCalcBounds();
    },

    layoutPrepare: function(order) {
        for (i in this.graph.nodes) {
            var node = this.graph.nodes[i];
            node.layoutPosX = 0;
            node.layoutPosY = 0;

        }
            var counter = 0;
            for (i in this.order) {
                var node = this.order[i];
                node.layoutPosX = counter;
                node.layoutPosY = Math.random();
                counter++;
            }

    },

    layoutCalcBounds: function() {
        var minx = Infinity, maxx = -Infinity, miny = Infinity, maxy = -Infinity;

        for (i in this.graph.nodes) {
            var x = this.graph.nodes[i].layoutPosX;
            var y = this.graph.nodes[i].layoutPosY;

            if(x > maxx) maxx = x;
            if(x < minx) minx = x;
            if(y > maxy) maxy = y;
            if(y < miny) miny = y;
        }

        this.graph.layoutMinX = minx;
        this.graph.layoutMaxX = maxx;

        this.graph.layoutMinY = miny;
        this.graph.layoutMaxY = maxy;
    }
};

/*
 * usefull JavaScript extensions,
 */

function log(a) {console.log&&console.log(a);}

/*
 * External addition to library. Translates appropriately to nodes that
 * do not completely fit within the canvas side.
 */
function translateInsideCanvas(shape, box, x, y, w, h) {
	 // Node extends past left side.
    if (x < 0) {
    	shape.translate(-x, 0);
    // Node extends past right side.
    } else if ((x + box.width + SelfLoopPadding) > w) {
    	shape.translate(-(x + box.width + SelfLoopPadding - w), 0);
    }
    
    // Node extends above top side.
    if (y < 0) {
    	shape.translate(0, -y);
    // Node extends below bottom.
    } else if ((y + box.height) > h) {
    	shape.translate(0, -(y + box.height - h));
    }
}

/*
 * Raphael Tooltip Plugin
 * - attaches an element as a tooltip to another element
 *
 * Usage example, adding a rectangle as a tooltip to a circle:
 *
 *      paper.circle(100,100,10).tooltip(paper.rect(0,0,20,30));
 *
 * If you want to use more shapes, you'll have to put them into a set.
 *
 */
Raphael.el.tooltip = function (tp) {
    this.tp = tp;
    this.tp.o = {x: 0, y: 0};
    this.tp.hide();
    this.hover(
        function(event){
            this.mousemove(function(event){
                this.tp.translate(event.clientX -
                                  this.tp.o.x,event.clientY - this.tp.o.y);
                this.tp.o = {x: event.clientX, y: event.clientY};
            });
            this.tp.show().toFront();
        },
        function(event){
            this.tp.hide();
            this.unmousemove();
        });
    return this;
};

/* For IE */
if (!Array.prototype.forEach)
{
  Array.prototype.forEach = function(fun /*, thisp*/)
  {
    var len = this.length;
    if (typeof fun != "function")
      throw new TypeError();

    var thisp = arguments[1];
    for (var i = 0; i < len; i++)
    {
      if (i in this)
        fun.call(thisp, this[i], i, this);
    }
  };
}
