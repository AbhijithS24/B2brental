/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.training.fulfilmentprocess.actions.order;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.orderprocessing.events.OrderCompletedEvent;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.ordersplitting.model.WarehouseModel;
import de.hybris.platform.processengine.action.AbstractProceduralAction;
import de.hybris.platform.servicelayer.event.EventService;

import de.hybris.platform.stock.StockService;
import de.hybris.platform.stock.impl.DefaultStockService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import java.util.ArrayList;
import java.util.stream.Collectors;


/**
 * Send event representing the completion of an order process.
 */
public class SendOrderCompletedNotificationAction extends AbstractProceduralAction<OrderProcessModel>
{
	private static final Logger LOG = Logger.getLogger(SendOrderCompletedNotificationAction.class);

	private EventService eventService;

	@Override
	public void executeAction(final OrderProcessModel process)
	{
		getEventService().publishEvent(new OrderCompletedEvent(process));
		if (LOG.isInfoEnabled())
		{
			LOG.info("Process: " + process.getCode() + " in step " + getClass());
		}
		StockService stockService = new DefaultStockService();
		final OrderModel order = process.getOrder();
		order.getEntries().stream().forEach(
				entry->
				{
					long orderedQty = entry.getQuantity();
					ProductModel product = entry.getProduct();
					final ArrayList<WarehouseModel> warehouseList = order.getConsignments().stream().map(consignmentModel -> consignmentModel.getWarehouse()).collect(Collectors.toCollection(ArrayList::new));
					warehouseList.stream().forEach(warehouse->stockService.release(product,warehouse,(int)orderedQty,""));
				}
		);

	}

	protected EventService getEventService()
	{
		return eventService;
	}

	@Required
	public void setEventService(final EventService eventService)
	{
		this.eventService = eventService;
	}
}
